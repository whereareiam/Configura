package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.MergeDefaultsResolver;
import me.whereareiam.configura.common.merge.resolver.MergeBehaviorResolver;
import me.whereareiam.configura.common.merge.resolver.MergePluginResolver;
import me.whereareiam.configura.common.merge.strategy.FieldMergeStrategyResolver;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.plugin.MergePluginRegistry;
import me.whereareiam.configura.merge.plugin.context.MergePluginContext;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategyRegistry;
import me.whereareiam.configura.type.UnknownFieldPolicy;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

public final class MergeCoordinator {
	private final ObjectMapper mapper;
	private final MergeBehaviorResolver behaviorResolver = new MergeBehaviorResolver();
	private final FieldMergeStrategyResolver strategyResolver;
	private final MergeDefaultsResolver defaultsResolver;
	private final MergePluginResolver fieldPluginResolver;

	public MergeCoordinator(
			ObjectMapper mapper,
			FieldMergeStrategyRegistry strategyRegistry,
			Class<? extends FieldMergeStrategy> defaultStrategy,
			MergeDefaultsResolver defaultsResolver,
			MergePluginRegistry pluginRegistry,
			MergePolicyResolverRegistry policyResolverRegistry
	) {
		this.mapper = mapper;
		this.strategyResolver = new FieldMergeStrategyResolver(defaultStrategy, strategyRegistry);
		this.defaultsResolver = defaultsResolver;
		this.fieldPluginResolver = new MergePluginResolver(pluginRegistry, policyResolverRegistry);
	}

	public @NotNull JsonNode mergeObject(
			JsonNode source,
			JsonNode defaults,
			Class<?> ownerType,
			boolean declaredKeysOnly,
			MergeBehavior currentBehavior,
			MergeOperation operation
	) {
		ObjectNode result = mapper.createObjectNode();
		ObjectNode sourceObject = source != null && source.isObject()
				? (ObjectNode) source
				: mapper.createObjectNode();
		ObjectNode defaultObject = defaults != null && defaults.isObject()
				? (ObjectNode) defaults
				: mapper.createObjectNode();

		Set<String> keys = new LinkedHashSet<>();
		sourceObject.fieldNames().forEachRemaining(keys::add);
		if (!declaredKeysOnly)
			defaultObject.fieldNames().forEachRemaining(keys::add);

		for (String key : keys) {
			JsonNode sourceValue = sourceObject.get(key);
			JsonNode defaultValue = defaultObject.get(key);
			if (sourceValue != null && sourceValue.isNull()) {
				result.set(key, sourceValue.deepCopy());
				continue;
			}

			MergePluginResolver.ResolvedField resolvedField = fieldPluginResolver.resolve(ownerType, key);
			if (defaultValue == null) {
				if (resolvedField.descriptor().getField() != null && sourceValue != null) {
					result.set(key, sourceValue.deepCopy());
					continue;
				}
				if (currentBehavior.getUnknownFieldPolicy() == UnknownFieldPolicy.PRESERVE && sourceValue != null)
					result.set(key, sourceValue.deepCopy());
				continue;
			}

			MergeBehavior propertyBehavior = behaviorResolver.resolve(
					currentBehavior,
					resolvedField.descriptor().getField(),
					resolvedField.childType()
			);
			if (resolvedField.policy().getBehaviorOverride() != null)
				propertyBehavior = resolvedField.policy().getBehaviorOverride();

			String fieldDescription = describe(ownerType, key, resolvedField.descriptor().getField());
			Class<? extends FieldMergeStrategy> strategyClass = strategyResolver.resolve(resolvedField.policy(), fieldDescription);
			JsonNode merged = resolvedField.plugin().merge(new MergePluginContext(
					mapper,
					resolvedField.descriptor(),
					resolvedField.policy(),
					resolvedField.childType(),
					sourceValue,
					defaultValue,
					strategyClass,
					operation.sourceDefaultsAsMissing(),
					propertyBehavior,
					(childSource, childDefaults, childOwnerType, childDeclaredOnly, childBehaviorOverride) ->
							mergeObject(childSource, childDefaults, childOwnerType, childDeclaredOnly, childBehaviorOverride, operation),
					type -> defaultsResolver.resolveInternal(type, operation.defaultsPolicy())
			));
			result.set(key, merged);
		}

		return result;
	}

	private String describe(Class<?> ownerType, String key, java.lang.reflect.Field field) {
		if (field != null)
			return field.getDeclaringClass().getName() + "#" + field.getName();
		return ownerType.getName() + "#" + key;
	}
}
