package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.resolver.MergePluginResolver;
import me.whereareiam.configura.common.util.PathNavigator;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.merge.plugin.MergePluginRegistry;
import me.whereareiam.configura.merge.plugin.context.MergePluginDefaultsContext;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;

import java.lang.reflect.Field;

public final class DefaultsCoordinator {
	private final ObjectMapper mapper;
	private final MergePluginResolver fieldPluginResolver;
	private final ModelDefaultsResolver modelDefaultsResolver;

	public DefaultsCoordinator(
			ObjectMapper mapper,
			MergePluginRegistry pluginRegistry,
			MergePolicyResolverRegistry policyResolverRegistry,
			ModelDefaultsResolver modelDefaultsResolver
	) {
		this.mapper = mapper;
		this.fieldPluginResolver = new MergePluginResolver(pluginRegistry, policyResolverRegistry);
		this.modelDefaultsResolver = modelDefaultsResolver;
	}

	public void applyFieldDefaults(ObjectNode node, Class<?> type, PrimitiveDefaultPolicy policy) {
		for (Field property : type.getDeclaredFields()) {
			String key = SerializedFieldResolver.resolveSerializedName(property);
			MergePluginResolver.ResolvedField resolvedField = fieldPluginResolver.resolve(type, property);
			JsonNode defaultValue = resolvedField.plugin().resolveDefaultValue(new MergePluginDefaultsContext(
					mapper,
					resolvedField.descriptor(),
					resolvedField.policy(),
					resolvedField.childType(),
					policy,
					childType -> this.modelDefaultsResolver.resolve(childType, policy)
			));
			if (defaultValue != null) {
				JsonNode existing = node.get(key);
				if (isMissing(existing, policy)) {
					node.set(key, defaultValue.deepCopy());
					continue;
				}
				if (existing.isObject() && defaultValue.isObject())
					deepFill((ObjectNode) existing, (ObjectNode) defaultValue, resolvedField.childType(), policy);
				continue;
			}

			if (hasFieldDefaults(resolvedField.childType())) {
				JsonNode existing = node.get(key);
				ObjectNode child = existing != null && existing.isObject() ? (ObjectNode) existing : mapper.createObjectNode();
				PathNavigator.write(node, key, child);
				applyFieldDefaults(child, resolvedField.childType(), policy);
			}
		}
	}

	private boolean hasFieldDefaults(Class<?> type) {
		for (Field property : type.getDeclaredFields()) {
			if (property.getAnnotation(Defaults.class) != null) return true;
		}

		return false;
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
				MergePluginResolver.ResolvedField resolvedField = fieldPluginResolver.resolve(type, key);
				deepFill((ObjectNode) existing, (ObjectNode) value, resolvedField.childType(), policy);
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
		JsonNode resolve(Class<?> type, PrimitiveDefaultPolicy policy);
	}
}
