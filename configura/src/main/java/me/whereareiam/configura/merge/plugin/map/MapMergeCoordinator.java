package me.whereareiam.configura.merge.plugin.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.strategy.DeepDefaults;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.NeverDefaults;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;
import me.whereareiam.configura.type.merge.tree.map.MapUnknownEntries;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

@RequiredArgsConstructor
public final class MapMergeCoordinator {
	private final ObjectMapper mapper;

	public @NotNull JsonNode merge(
			Field property,
			JsonNode source,
			JsonNode defaults,
			Class<?> valueType,
			Class<? extends FieldMergeStrategy> strategyClass,
			MergeBehavior propertyBehavior,
			MapMergeConfig config,
			MergeContext.RecursiveMerge recursiveMerge
	) {
		if (strategyClass == null || SourceOwnsField.class.equals(strategyClass))
			return sourceOwnsMap(source, defaults);
		if (NeverDefaults.class.equals(strategyClass))
			return source == null ? mapper.nullNode() : source.deepCopy();
		if (!DeepDefaults.class.equals(strategyClass))
			throw new ConfigException("Field " + describe(property) + " uses unsupported merge strategy " + strategyClass.getName() + " with @MergeMap");

		if (source != null && !source.isNull() && !source.isObject())
			throw wrongNodeType(property, "map/object", source);
		if (defaults != null && !defaults.isNull() && !defaults.isObject())
			throw wrongNodeType(property, "map/object", defaults);

		ObjectNode result = mapper.createObjectNode();
		ObjectNode sourceObject = source instanceof ObjectNode object ? object : mapper.createObjectNode();
		ObjectNode defaultObject = defaults instanceof ObjectNode object ? object : mapper.createObjectNode();
		Set<String> seen = new LinkedHashSet<>();

		sourceObject.fieldNames().forEachRemaining(key -> {
			seen.add(key);
			JsonNode sourceEntry = sourceObject.get(key);
			JsonNode defaultEntry = defaultObject.get(key);
			if (defaultEntry == null && config.getUnknownEntries() == MapUnknownEntries.REJECT)
				throw new ConfigException("Field " + describe(property) + " does not allow unknown map entry key '" + key + "'");

			JsonNode merged = mergeEntry(sourceEntry, defaultEntry, valueType, propertyBehavior, recursiveMerge);
			result.set(key, merged);
		});

		if (seedsDefaultEntries(config) || source == null || source.isNull()) {
			defaultObject.fieldNames().forEachRemaining(key -> {
				if (seen.contains(key)) return;
				result.set(key, mergeEntry(null, defaultObject.get(key), valueType, propertyBehavior, recursiveMerge));
			});
		}

		return result;
	}

	private boolean seedsDefaultEntries(MapMergeConfig config) {
		return config.getPresence() == me.whereareiam.configura.type.merge.tree.map.MapPresence.SEED_DEFAULTS
				|| config.getPresence() == me.whereareiam.configura.type.merge.tree.map.MapPresence.DEFAULT_DOMAIN_ONLY;
	}

	private JsonNode mergeEntry(
			JsonNode sourceEntry,
			JsonNode defaultEntry,
			Class<?> valueType,
			MergeBehavior propertyBehavior,
			MergeContext.RecursiveMerge recursiveMerge
	) {
		if (defaultEntry == null)
			return sourceEntry == null ? mapper.nullNode() : sourceEntry.deepCopy();
		if (defaultEntry.isObject())
			return recursiveMerge.merge(sourceEntry, defaultEntry, valueType, false, propertyBehavior);
		if (sourceEntry != null && !sourceEntry.isNull())
			return sourceEntry.deepCopy();
		return defaultEntry.deepCopy();
	}

	private JsonNode sourceOwnsMap(JsonNode source, JsonNode defaults) {
		if (source != null && !source.isNull())
			return source.deepCopy();
		if (defaults == null) return mapper.createObjectNode();
		return defaults.deepCopy();
	}

	private ConfigException wrongNodeType(Field field, String expected, JsonNode actual) {
		return new ConfigException("Field " + describe(field) + " expected a " + expected + " node but found " + actual.getNodeType());
	}

	private String describe(Field field) {
		return field.getDeclaringClass().getName() + "#" + field.getName();
	}
}
