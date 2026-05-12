package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.DefaultMergeDefaultsRegistry;
import me.whereareiam.configura.common.merge.defaults.MergeDefaultsResolver;
import me.whereareiam.configura.common.merge.strategy.MergeStrategyFactory;
import me.whereareiam.configura.common.merge.strategy.MergeStrategyResolver;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.MergePolicy;
import me.whereareiam.configura.merge.strategy.MergeStrategy;
import me.whereareiam.configura.merge.strategy.MergeStrategyRegistry;
import me.whereareiam.configura.merge.strategy.type.DeepDefaults;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MergeEngine {
	private final ObjectMapper mapper;
	private final MergeDefaultsResolver defaultsResolver;
	private final MergeStrategyResolver strategyResolver;
	private final MergeStrategyFactory strategyFactory = new MergeStrategyFactory();

	public MergeEngine(
			ObjectMapper mapper,
			DefaultMergeDefaultsRegistry defaultsRegistry,
			MergeStrategyRegistry strategyRegistry,
			Class<? extends MergeStrategy> defaultStrategy
	) {
		this.mapper = mapper;
		this.defaultsResolver = new MergeDefaultsResolver(mapper, defaultsRegistry);
		this.strategyResolver = new MergeStrategyResolver(defaultStrategy, strategyRegistry);
	}

	public <T> ObjectNode defaultsNode(T model, Class<T> type, Mode mode) {
		return defaultsNode(model, type, resolvePolicy(mode));
	}

	public <T> ObjectNode merge(JsonNode source, T model, Class<T> type, Mode mode) {
		return merge(source, model, type, resolvePolicy(mode));
	}

	public <T> ObjectNode defaultsNode(T model, Class<T> type, MergePolicy policy) {
		return defaultsResolver.resolve(model, type, policy.primitiveDefaultPolicy());
	}

	public <T> ObjectNode merge(JsonNode source, T model, Class<T> type, MergePolicy policy) {
		ObjectNode defaults = defaultsNode(model, type, policy);
		JsonNode merged = mergeObject(source, defaults, type, false, policy);
		return merged instanceof ObjectNode objectNode ? objectNode : mapper.createObjectNode();
	}

	private JsonNode mergeObject(JsonNode source, JsonNode defaults, Class<?> ownerType, boolean declaredKeysOnly, MergePolicy policy) {
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
			if (defaultValue == null) {
				if (sourceValue != null) result.set(key, sourceValue.deepCopy());
				continue;
			}

			Field field = MergeFieldResolver.resolveField(ownerType, key);
			Class<?> childType = MergeFieldResolver.resolveChildType(field, ownerType);
			Class<? extends MergeStrategy> strategyClass = strategyResolver.resolve(field);
			MergeContext context = new MergeContext(
					mapper,
					ownerType,
					field,
					key,
					childType,
					sourceValue,
					defaultValue,
					policy.primitiveDefaultPolicy() == PrimitiveDefaultPolicy.AS_MISSING,
					(childSource, childDefaults, childOwnerType, childDeclaredOnly) -> mergeObject(childSource, childDefaults, childOwnerType, childDeclaredOnly, policy)
			);

			JsonNode merged = strategyFactory.create(strategyClass != null ? strategyClass : DeepDefaults.class).merge(context);
			if (merged != null) result.set(key, merged);
		}

		return result;
	}

	private MergePolicy resolvePolicy(Mode mode) {
		return mode == Mode.DEFAULT_INSTANCE ? MergePolicy.update() : MergePolicy.save();
	}

	public enum Mode {
		DEFAULT_INSTANCE,
		USER_MODEL
	}
}
