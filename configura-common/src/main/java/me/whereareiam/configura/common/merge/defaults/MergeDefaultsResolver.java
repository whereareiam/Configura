package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.type.ClassProviderDefaultsResolver;
import me.whereareiam.configura.common.merge.resolver.MergePluginResolver;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.merge.plugin.MergePluginRegistry;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;

import java.lang.reflect.Field;

public final class MergeDefaultsResolver {
	private final MergeDefaultsProviderRegistry defaultsRegistry;
	private final ObjectMapper mapper;
	private final DefaultsCoordinator defaultsCoordinator;
	private final MergePluginResolver fieldPluginResolver;

	public MergeDefaultsResolver(
			ObjectMapper mapper,
			MergeDefaultsProviderRegistry defaultsRegistry,
			MergePluginRegistry pluginRegistry,
			MergePolicyResolverRegistry policyResolverRegistry
	) {
		this.mapper = mapper;
		this.defaultsRegistry = defaultsRegistry;
		this.defaultsCoordinator = new DefaultsCoordinator(mapper, pluginRegistry, policyResolverRegistry, this::resolveInternal);
		this.fieldPluginResolver = new MergePluginResolver(pluginRegistry, policyResolverRegistry);
	}

	public <T> ObjectNode resolve(T model, Class<T> type, PrimitiveDefaultPolicy primitiveDefaultPolicy) {
		ObjectNode node = mapper.valueToTree(model);
		applyClassDefaults(node, type, primitiveDefaultPolicy);
		defaultsCoordinator.applyFieldDefaults(node, type, primitiveDefaultPolicy);
		return node;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public ObjectNode resolveInternal(Class<?> type, PrimitiveDefaultPolicy primitiveDefaultPolicy) {
		return resolve(instantiate(type), (Class) type, primitiveDefaultPolicy);
	}

	private void applyClassDefaults(ObjectNode target, Class<?> type, PrimitiveDefaultPolicy policy) {
		JsonNode classDefaults = new ClassProviderDefaultsResolver(defaultsRegistry).resolve(mapper, type);
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
				Class<?> childType = fieldPluginResolver.resolve(type, key).childType();
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

	private static Object instantiate(Class<?> type) {
		try {
			var constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception exception) {
			throw new IllegalStateException("Cannot instantiate defaults target: " + type.getName(), exception);
		}
	}
}
