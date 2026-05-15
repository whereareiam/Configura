package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.DefaultMergeDefaultsRegistry;
import me.whereareiam.configura.common.merge.defaults.MergeDefaultsResolver;
import me.whereareiam.configura.common.merge.strategy.MergeStrategyFactory;
import me.whereareiam.configura.common.merge.strategy.MergeStrategyResolver;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.strategy.MergeStrategy;
import me.whereareiam.configura.merge.strategy.MergeStrategyRegistry;
import me.whereareiam.configura.merge.strategy.type.DeepDefaults;
import me.whereareiam.configura.type.UnknownFieldPolicy;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MergeEngine {
	private final ObjectMapper mapper;
	private final MergeDefaultsResolver defaultsResolver;
	private final MergeStrategyResolver strategyResolver;
	private final MergeStrategyFactory strategyFactory = new MergeStrategyFactory();
	private final MergeBehaviorResolver behaviorResolver = new MergeBehaviorResolver();
	private final MergeBehavior behavior;

	public MergeEngine(
			ObjectMapper mapper,
			DefaultMergeDefaultsRegistry defaultsRegistry,
			MergeStrategyRegistry strategyRegistry,
			Class<? extends MergeStrategy> defaultStrategy,
			MergeBehavior behavior
	) {
		this.mapper = mapper;
		this.defaultsResolver = new MergeDefaultsResolver(mapper, defaultsRegistry);
		this.strategyResolver = new MergeStrategyResolver(defaultStrategy, strategyRegistry);
		this.behavior = behavior != null ? behavior : MergeBehavior.defaults();
	}

	public <T> ObjectNode defaultsNode(T model, Class<T> type) {
		return defaultsNodeInternal(model, type, MergeOperation.userModel());
	}

	public <T> ObjectNode mergeUserModel(JsonNode source, T model, Class<T> type) {
		return mergeInternal(source, model, type, MergeOperation.userModel());
	}

	public <T> ObjectNode mergeDefaults(JsonNode source, T model, Class<T> type) {
		return mergeInternal(source, model, type, MergeOperation.syntheticDefaults(behavior));
	}

	private <T> ObjectNode defaultsNodeInternal(T model, Class<T> type, MergeOperation operation) {
		return defaultsResolver.resolve(model, type, operation.defaultsPolicy());
	}

	private <T> ObjectNode mergeInternal(JsonNode source, T model, Class<T> type, MergeOperation operation) {
		ObjectNode defaults = defaultsNodeInternal(model, type, operation);
		JsonNode merged = mergeObject(source, defaults, type, false, behaviorResolver.resolve(behavior, type), operation);
		return merged instanceof ObjectNode objectNode ? objectNode : mapper.createObjectNode();
	}

	private JsonNode mergeObject(
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
			Field field = MergeFieldResolver.resolveField(ownerType, key);
			if (sourceValue != null && sourceValue.isNull()) {
				result.set(key, sourceValue.deepCopy());
				continue;
			}
			if (defaultValue == null) {
				if (field != null && sourceValue != null) {
					result.set(key, sourceValue.deepCopy());
					continue;
				}
				if (currentBehavior.getUnknownFieldPolicy() == UnknownFieldPolicy.PRESERVE && sourceValue != null)
					result.set(key, sourceValue.deepCopy());
				continue;
			}

			Class<?> childType = MergeFieldResolver.resolveChildType(field, ownerType);
			MergeBehavior childBehavior = behaviorResolver.resolve(currentBehavior, field, childType);
			Class<? extends MergeStrategy> strategyClass = strategyResolver.resolve(field);
			MergeContext context = new MergeContext(
					mapper,
					ownerType,
					field,
					key,
					childType,
					sourceValue,
					defaultValue,
					operation.sourceDefaultsAsMissing(),
					childBehavior,
					(childSource, childDefaults, childOwnerType, childDeclaredOnly, childBehaviorOverride) ->
							mergeObject(childSource, childDefaults, childOwnerType, childDeclaredOnly, childBehaviorOverride, operation)
			);

			JsonNode merged = strategyFactory.create(strategyClass != null ? strategyClass : DeepDefaults.class).merge(context);
			if (merged != null) result.set(key, merged);
		}

		return result;
	}
}
