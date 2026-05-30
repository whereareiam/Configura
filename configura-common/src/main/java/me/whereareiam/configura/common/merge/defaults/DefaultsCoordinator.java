package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.resolver.MergeTypeAdapterResolver;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.document.DocumentProcessor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.merge.defaults.DefaultsResolverRegistry;
import me.whereareiam.configura.merge.defaults.MergeModelDefaultsResolver;
import me.whereareiam.configura.merge.defaults.context.DefaultsContext;
import me.whereareiam.configura.merge.defaults.descriptor.DefaultsDescriptor;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

public final class DefaultsCoordinator {
	private final ObjectMapper mapper;
	private final MergeTypeAdapterResolver fieldAdapterResolver;
	private final DefaultsResolverRegistry defaultsResolverRegistry;
	private final DocumentProcessor documentRuntime;
	private final ModelDefaultsResolver modelDefaultsResolver;

	public DefaultsCoordinator(
			ObjectMapper mapper,
			DefaultsResolverRegistry defaultsResolverRegistry,
			me.whereareiam.configura.merge.type.MergeTypeAdapterRegistry adapterRegistry,
			MergePolicyResolverRegistry policyResolverRegistry,
			DocumentProcessor documentRuntime,
			ModelDefaultsResolver modelDefaultsResolver
	) {
		this.mapper = mapper;
		this.fieldAdapterResolver = new MergeTypeAdapterResolver(adapterRegistry, policyResolverRegistry);
		this.defaultsResolverRegistry = defaultsResolverRegistry;
		this.documentRuntime = documentRuntime;
		this.modelDefaultsResolver = modelDefaultsResolver;
	}

	public void applyFieldDefaults(
			ObjectNode node,
			Class<?> type,
			PrimitiveDefaultPolicy policy
	) {
		for (Field property : type.getDeclaredFields()) {
			if (!isDefaultEligible(property)) continue;

			String key = SerializedFieldResolver.resolveSerializedName(property);
			MergeTypeAdapterResolver.ResolvedField resolvedField = fieldAdapterResolver.resolve(type, property);
			JsonNode existing = node.get(key);
			DocumentTypeContext childContext = childContext(
					existing,
					node,
					property,
					key
			);
			Class<?> effectiveChildType = documentRuntime.resolveType(resolvedField.childType(), childContext);
			DefaultsDescriptor descriptor = new DefaultsDescriptor(
					mapper,
					resolvedField.descriptor().getOwnerType(),
					resolvedField.descriptor().getField(),
					resolvedField.descriptor().getSerializedName(),
					resolvedField.descriptor().getDeclaredType(),
					resolvedField.descriptor().getGenericType()
			);
			JsonNode defaultValue = defaultsResolverRegistry.resolve(
					descriptor,
					new DefaultsContext(
							mapper,
							descriptor,
							effectiveChildType,
							policy,
							new MergeModelDefaultsResolver() {
								@Override
								public JsonNode resolve(@NotNull Class<?> childType) {
									return modelDefaultsResolver.resolve(childType, policy, childContext);
								}

								@Override
								public JsonNode resolve(@NotNull Class<?> childType, DocumentTypeContext context) {
									DocumentTypeContext effectiveContext = context != null ? context : childContext;
									return modelDefaultsResolver.resolve(childType, policy, effectiveContext);
								}
							}
					)
			);
			if (defaultValue == null && isDirectObjectField(property)) {
				defaultValue = this.modelDefaultsResolver.resolve(effectiveChildType, policy, childContext);
			}

			if (defaultValue != null) {
				if (isMissing(existing, policy)) {
					node.set(key, defaultValue.deepCopy());
					continue;
				}
				if (existing.isObject() && defaultValue.isObject()) {
					deepFill((ObjectNode) existing, (ObjectNode) defaultValue, effectiveChildType, policy, node);
				}
            }
		}
	}

	private boolean isDefaultEligible(Field property) {
		int modifiers = property.getModifiers();
		return !property.isSynthetic()
				&& !Modifier.isStatic(modifiers)
				&& !Modifier.isTransient(modifiers);
	}

	private void deepFill(
			ObjectNode target,
			ObjectNode defaults,
			Class<?> type,
			PrimitiveDefaultPolicy policy,
			ObjectNode parentNode
	) {
		for (var entry : defaults.properties()) {
			String key = entry.getKey();
			JsonNode existing = target.get(key);
			JsonNode value = entry.getValue();

			if (isMissing(existing, policy)) {
				target.set(key, value.deepCopy());
				continue;
			}

			if (existing.isObject() && value.isObject()) {
				MergeTypeAdapterResolver.ResolvedField resolvedField = fieldAdapterResolver.resolve(type, key);
				DocumentTypeContext childContext = childContext(
						existing,
						parentNode,
						resolvedField.descriptor().getField(),
						key
				);
				Class<?> effectiveChildType = documentRuntime.resolveType(resolvedField.childType(), childContext);
				deepFill(
						(ObjectNode) existing,
						(ObjectNode) value,
						effectiveChildType,
						policy,
						target
				);
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

	public interface ModelDefaultsResolver {
		JsonNode resolve(Class<?> type, PrimitiveDefaultPolicy policy, DocumentTypeContext context);
	}

	private boolean isDirectObjectField(Field field) {
		Class<?> fieldType = field.getType();
		return !fieldType.isArray()
				&& !Collection.class.isAssignableFrom(fieldType)
				&& !java.util.Map.class.isAssignableFrom(fieldType);
	}

	private DocumentTypeContext childContext(
			JsonNode currentNode,
			JsonNode parentNode,
			Field field,
			String fieldName
	) {
		return new DocumentTypeContext(
				currentNode,
				parentNode,
				field,
				fieldName,
				null,
				null
		);
	}
}
