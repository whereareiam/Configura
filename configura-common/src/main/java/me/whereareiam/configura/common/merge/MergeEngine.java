package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.DefaultMergeDefaultsRegistry;
import me.whereareiam.configura.common.merge.defaults.MergeDefaultsResolver;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.MergeStrategy;
import me.whereareiam.configura.merge.MergeStrategyRegistry;
import me.whereareiam.configura.merge.strategy.DeepDefaults;

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
		return defaultsResolver.resolve(model, type, mode);
	}

	public <T> ObjectNode merge(JsonNode source, T model, Class<T> type, Mode mode) {
		ObjectNode defaults = defaultsNode(model, type, mode);
		JsonNode merged = mergeObject(source, defaults, type, false, mode);
		return merged instanceof ObjectNode objectNode ? objectNode : mapper.createObjectNode();
	}

	private JsonNode mergeObject(JsonNode source, JsonNode defaults, Class<?> ownerType, boolean declaredKeysOnly, Mode mode) {
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
					mode == Mode.DEFAULT_INSTANCE,
					(childSource, childDefaults, childOwnerType, childDeclaredOnly) -> mergeObject(childSource, childDefaults, childOwnerType, childDeclaredOnly, mode)
			);

			JsonNode merged = strategyFactory.create(strategyClass != null ? strategyClass : DeepDefaults.class).merge(context);
			if (merged != null) result.set(key, merged);
		}

		return result;
	}

	public enum Mode {
		DEFAULT_INSTANCE,
		USER_MODEL
	}
}
