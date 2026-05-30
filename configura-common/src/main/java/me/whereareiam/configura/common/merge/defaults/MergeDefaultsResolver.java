package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.type.ClassProviderDefaultsResolver;
import me.whereareiam.configura.common.merge.resolver.MergeTypeAdapterResolver;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.document.DocumentProcessor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.merge.defaults.DefaultsResolverRegistry;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class MergeDefaultsResolver {
	private final ObjectMapper mapper;
	private final DefaultsCoordinator defaultsCoordinator;
	private final MergeTypeAdapterResolver fieldAdapterResolver;
	private final DocumentProcessor documentRuntime;
	private final ClassProviderDefaultsResolver classProviderDefaultsResolver;

	public MergeDefaultsResolver(
			ObjectMapper mapper,
			DefaultsProviderRegistry defaultsRegistry,
			DocumentProcessor documentRuntime,
			DefaultsResolverRegistry defaultsResolverRegistry,
			me.whereareiam.configura.merge.type.MergeTypeAdapterRegistry adapterRegistry,
			MergePolicyResolverRegistry policyResolverRegistry
	) {
		this.mapper = mapper;
		this.documentRuntime = documentRuntime;
		this.classProviderDefaultsResolver = new ClassProviderDefaultsResolver(defaultsRegistry);
		this.defaultsCoordinator = new DefaultsCoordinator(
				mapper,
				defaultsResolverRegistry,
				adapterRegistry,
				policyResolverRegistry,
				documentRuntime,
				this::resolveInternal
		);
		this.fieldAdapterResolver = new MergeTypeAdapterResolver(adapterRegistry, policyResolverRegistry);
	}

	public <T> ObjectNode resolve(T model, Class<T> type, PrimitiveDefaultPolicy primitiveDefaultPolicy) {
		return resolve(model, type, primitiveDefaultPolicy, null);
	}

	public <T> ObjectNode resolve(
			T model,
			Class<T> type,
			PrimitiveDefaultPolicy primitiveDefaultPolicy,
			DocumentTypeContext context
	) {
		Class<?> effectiveType = documentRuntime.resolveType(type, context);
		JsonNode tree = mapper.valueToTree(model);
		ObjectNode node = tree instanceof ObjectNode objectNode ? objectNode : mapper.createObjectNode();
		applyClassDefaults(node, effectiveType, primitiveDefaultPolicy);
		defaultsCoordinator.applyFieldDefaults(node, effectiveType, primitiveDefaultPolicy);
		return node;
	}

	public ObjectNode resolveInternal(Class<?> type, PrimitiveDefaultPolicy primitiveDefaultPolicy) {
		return resolveInternal(type, primitiveDefaultPolicy, null);
	}

	public @Nullable ObjectNode resolveInternal(
			Class<?> type,
			PrimitiveDefaultPolicy primitiveDefaultPolicy,
			DocumentTypeContext context
	) {
		Class<?> effectiveType = documentRuntime.resolveType(type, context);
		if (!canResolveModelDefaults(effectiveType) || !hasModelDefaults(effectiveType))
			return null;

		ObjectNode node = instantiateDefaultsNode(effectiveType);
		applyClassDefaults(node, effectiveType, primitiveDefaultPolicy);
		defaultsCoordinator.applyFieldDefaults(node, effectiveType, primitiveDefaultPolicy);
		return node.isEmpty() ? null : node;
	}

	private void applyClassDefaults(ObjectNode target, Class<?> type, PrimitiveDefaultPolicy policy) {
		JsonNode classDefaults = classProviderDefaultsResolver.resolve(mapper, type);
		if (classDefaults == null || !classDefaults.isObject()) return;
		deepFill(target, (ObjectNode) classDefaults, type, policy);
	}

	private void deepFill(ObjectNode target, ObjectNode defaults, Class<?> type, PrimitiveDefaultPolicy policy) {
		for (var entry : defaults.properties()) {
			String key = entry.getKey();
			JsonNode existing = target.get(key);
			JsonNode value = entry.getValue();
			if (isMissing(existing, policy)) {
				target.set(key, value.deepCopy());
				continue;
			}
			if (existing.isObject() && value.isObject()) {
				Field property = SerializedFieldResolver.resolveField(type, key);
				Class<?> childType = fieldAdapterResolver.resolve(type, key).childType();
				if (property == null && childType == type) {
					deepFill((ObjectNode) existing, (ObjectNode) value, type, policy);
					continue;
				}
				deepFill((ObjectNode) existing, (ObjectNode) value, childType, policy);
			}
		}
	}

	private boolean isMissing(JsonNode node, PrimitiveDefaultPolicy policy) {
		if (node == null || node.isNull()) return true;
		if (policy != PrimitiveDefaultPolicy.AS_MISSING) return false;

		return (node.isNumber() && node.asDouble() == 0.0)
				|| (node.isBoolean() && !node.asBoolean())
				|| (node.isArray() && node.isEmpty());
	}

	private ObjectNode instantiateDefaultsNode(Class<?> type) {
		if (!hasNoArgsConstructor(type)) return mapper.createObjectNode();

		JsonNode tree = mapper.valueToTree(instantiate(type));
		if (!(tree instanceof ObjectNode objectNode)) return mapper.createObjectNode();

		removeNullFields(objectNode);
		return objectNode;
	}

	private boolean hasModelDefaults(Class<?> type) {
		if (classProviderDefaultsResolver.hasDefaults(type)) return true;

		for (Field property : type.getDeclaredFields()) {
			if (isDefaultEligible(property)) return true;
		}

		return false;
	}

	private boolean isDefaultEligible(Field property) {
		int modifiers = property.getModifiers();
		return !property.isSynthetic()
				&& !Modifier.isStatic(modifiers)
				&& !Modifier.isTransient(modifiers);
	}

	private static boolean canResolveModelDefaults(Class<?> type) {
		if (type == null || type.isPrimitive() || type.isArray() || type.isEnum()) return false;
		if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) return false;
		if (type == Object.class || JsonNode.class.isAssignableFrom(type)) return false;

		return !CharSequence.class.isAssignableFrom(type)
				&& !Number.class.isAssignableFrom(type)
				&& Boolean.class != type
				&& Character.class != type
				&& !Collection.class.isAssignableFrom(type)
				&& !Map.class.isAssignableFrom(type)
				&& !type.getPackageName().startsWith("java.time");
	}

	private static boolean hasNoArgsConstructor(Class<?> type) {
		try {
			type.getDeclaredConstructor();
			return true;
		} catch (NoSuchMethodException exception) {
			return false;
		}
	}

	private static Object instantiate(Class<?> type) {
		try {
			var constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception exception) {
			throw new IllegalStateException("Cannot instantiate defaults target: " + type.getName(), exception);
		}
	}

	private static void removeNullFields(ObjectNode node) {
		var names = node.fieldNames();
		List<String> nullFields = new ArrayList<>();
		while (names.hasNext()) {
			String name = names.next();
			if (node.path(name).isNull()) nullFields.add(name);
		}
		nullFields.forEach(node::remove);
	}
}
