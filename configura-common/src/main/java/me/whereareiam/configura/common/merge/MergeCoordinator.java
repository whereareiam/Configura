package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.MergeDefaultsResolver;
import me.whereareiam.configura.common.merge.resolver.MergeBehaviorResolver;
import me.whereareiam.configura.common.merge.resolver.MergeStrategyResolver;
import me.whereareiam.configura.common.merge.resolver.MergeTypeAdapterResolver;
import me.whereareiam.configura.common.merge.strategy.ResolvedMergeStrategy;
import me.whereareiam.configura.document.DocumentProcessor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.defaults.MergeModelDefaultsResolver;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.MergeStrategyRegistry;
import me.whereareiam.configura.merge.type.MergeTypeAdapterRegistry;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.type.UnknownFieldPolicy;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MergeCoordinator {
	private final ObjectMapper mapper;
	private final MergeBehaviorResolver behaviorResolver = new MergeBehaviorResolver();
	private final MergeStrategyResolver strategyResolver;
	private final MergeDefaultsResolver defaultsResolver;
	private final DocumentProcessor documentRuntime;
	private final MergeTypeAdapterResolver fieldAdapterResolver;

	public MergeCoordinator(
			ObjectMapper mapper,
			MergeStrategyRegistry strategyRegistry,
			Class<? extends FieldMergeStrategy> defaultStrategy,
			String defaultStrategyName,
			MergeDefaultsResolver defaultsResolver,
			DocumentProcessor documentRuntime,
			MergeTypeAdapterRegistry adapterRegistry,
			MergePolicyResolverRegistry policyResolverRegistry
	) {
		this.mapper = mapper;
		this.strategyResolver = new MergeStrategyResolver(defaultStrategy, defaultStrategyName, strategyRegistry);
		this.defaultsResolver = defaultsResolver;
		this.documentRuntime = documentRuntime;
		this.fieldAdapterResolver = new MergeTypeAdapterResolver(adapterRegistry, policyResolverRegistry);
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
		JsonNode currentNode = !sourceObject.isEmpty()
				? sourceObject
				: defaultObject;

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

			MergeTypeAdapterResolver.ResolvedField resolvedField = fieldAdapterResolver.resolve(ownerType, key);
			if (defaultValue == null) {
				if (resolvedField.descriptor().getField() != null && sourceValue != null) {
					result.set(key, sourceValue.deepCopy());
					continue;
				}
				if (currentBehavior.getUnknownFieldPolicy() == UnknownFieldPolicy.PRESERVE && sourceValue != null) {
					result.set(key, sourceValue.deepCopy());
				}

				continue;
			}

			DocumentTypeContext childContext = new DocumentTypeContext(
					sourceValue != null ? sourceValue : defaultValue,
					currentNode,
					resolvedField.descriptor().getField(),
					key,
					null,
					null
			);
			Class<?> effectiveChildType = documentRuntime.resolveType(resolvedField.childType(), childContext);

			MergeBehavior propertyBehavior = behaviorResolver.resolve(
					currentBehavior,
					resolvedField.descriptor().getField(),
					effectiveChildType
			);
			if (resolvedField.policy().getBehaviorOverride() != null)
				propertyBehavior = resolvedField.policy().getBehaviorOverride();

			String fieldDescription = describe(ownerType, key, resolvedField.descriptor().getField());
			ResolvedMergeStrategy strategy = strategyResolver.resolve(resolvedField.policy(), fieldDescription);
			JsonNode merged = resolvedField.adapter().merge(new MergeTypeAdapterContext(
					mapper,
					resolvedField.descriptor(),
					resolvedField.policy(),
					effectiveChildType,
					sourceValue,
					defaultValue,
					strategy.getDefinition(),
					strategy.getStrategy(),
					operation.sourceDefaultsAsMissing(),
					propertyBehavior,
					(childSource, childDefaults, childOwnerType, childDeclaredOnly, childBehaviorOverride) ->
							mergeObject(childSource, childDefaults, childOwnerType, childDeclaredOnly, childBehaviorOverride, operation),
					new MergeModelDefaultsResolver() {
						@Override
						public JsonNode resolve(@NotNull Class<?> type) {
							return defaultsResolver.resolveInternal(type, operation.defaultsPolicy());
						}

						@Override
						public JsonNode resolve(@NotNull Class<?> type, DocumentTypeContext context) {
							return defaultsResolver.resolveInternal(type, operation.defaultsPolicy(), context);
						}
					},
					documentRuntime::resolveType
			));

			if (merged != null && !merged.isNull())
				result.set(key, merged);
		}

		return result;
	}

	private String describe(Class<?> ownerType, String key, Field field) {
		if (field != null) return field.getDeclaringClass().getName() + "#" + field.getName();
		return ownerType.getName() + "#" + key;
	}
}
