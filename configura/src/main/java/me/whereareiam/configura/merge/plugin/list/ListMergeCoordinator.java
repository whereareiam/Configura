package me.whereareiam.configura.merge.plugin.list;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.strategy.DeepDefaults;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.NeverDefaults;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;
import me.whereareiam.configura.type.merge.tree.list.ListMode;
import me.whereareiam.configura.type.merge.tree.list.ListUnknownEntries;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public final class ListMergeCoordinator {
	private final ObjectMapper mapper;

	public @NotNull JsonNode merge(
			Field property,
			JsonNode source,
			JsonNode defaults,
			JsonNode genericDefaults,
			Class<?> entryType,
			Class<? extends FieldMergeStrategy> strategyClass,
			MergeBehavior propertyBehavior,
			ListMergeConfig config,
			MergeContext.RecursiveMerge recursiveMerge
	) {
		if (strategyClass == null || SourceOwnsField.class.equals(strategyClass))
			return sourceOwnsList(source, defaults);
		if (NeverDefaults.class.equals(strategyClass))
			return source == null ? mapper.nullNode() : source.deepCopy();
		if (!DeepDefaults.class.equals(strategyClass))
			throw new ConfigException("Field " + describe(property) + " uses unsupported merge strategy " + strategyClass.getName() + " with @MergeList");

		if (config.getMode() == ListMode.PLAIN)
			return mergePlainList(source, defaults, property);

		return mergeKeyedList(source, defaults, genericDefaults, property, entryType, propertyBehavior, config, recursiveMerge);
	}

	private JsonNode mergePlainList(JsonNode source, JsonNode defaults, Field property) {
		if (source == null || source.isNull())
			return defaults == null ? mapper.createArrayNode() : defaults.deepCopy();
		if (!source.isArray())
			throw new ConfigException("Field " + describe(property) + " expected a list node but found " + source.getNodeType());
		return source.deepCopy();
	}

	private JsonNode mergeKeyedList(
			JsonNode source,
			JsonNode defaults,
			JsonNode genericDefaults,
			Field property,
			Class<?> entryType,
			MergeBehavior propertyBehavior,
			ListMergeConfig config,
			MergeContext.RecursiveMerge recursiveMerge
	) {
		if (source != null && !source.isNull() && !source.isArray())
			throw wrongNodeType(property, source);
		if (defaults != null && !defaults.isNull() && !defaults.isArray())
			throw wrongNodeType(property, defaults);

		ArrayNode result = mapper.createArrayNode();
		ArrayNode sourceArray = source instanceof ArrayNode array ? array : mapper.createArrayNode();
		ArrayNode defaultArray = defaults instanceof ArrayNode array ? array : mapper.createArrayNode();
		LinkedHashMap<String, JsonNode> defaultEntries = index(defaultArray, property, config.getKey(), "defaults");
		LinkedHashMap<String, JsonNode> sourceEntries = index(sourceArray, property, config.getKey(), "source");

		for (Map.Entry<String, JsonNode> entry : sourceEntries.entrySet()) {
			String key = entry.getKey();
			JsonNode sourceEntry = entry.getValue();
			JsonNode defaultEntry = defaultEntries.get(key);
			if (defaultEntry == null && config.getUnknownEntries() == ListUnknownEntries.REJECT)
				throw new ConfigException("Field " + describe(property) + " does not allow unknown list entry key '" + key + "'");

			JsonNode mergedDefaults = mergeEntryDefaults(genericDefaults, defaultEntry, entryType, propertyBehavior, recursiveMerge);
			if (!(mergedDefaults instanceof ObjectNode mergedObject))
				throw new ConfigException("Field " + describe(property) + " requires object-like keyed list entries");
			result.add(recursiveMerge.merge(sourceEntry, mergedObject, entryType, false, propertyBehavior));
		}

		if (seedsDefaultEntries(config) || source == null || source.isNull()) {
			for (Map.Entry<String, JsonNode> entry : defaultEntries.entrySet()) {
				if (sourceEntries.containsKey(entry.getKey())) continue;
				JsonNode merged = mergeEntryDefaults(genericDefaults, entry.getValue(), entryType, propertyBehavior, recursiveMerge);
				if (!(merged instanceof ObjectNode))
					throw new ConfigException("Field " + describe(property) + " requires object-like keyed list entries");
				result.add(merged);
			}
		}

		return result;
	}

	private boolean seedsDefaultEntries(ListMergeConfig config) {
		return config.getPresence() == me.whereareiam.configura.type.merge.tree.list.ListPresence.SEED_DEFAULTS
				|| config.getPresence() == me.whereareiam.configura.type.merge.tree.list.ListPresence.DEFAULT_DOMAIN_ONLY;
	}

	private @NotNull LinkedHashMap<String, JsonNode> index(
			ArrayNode entries,
			Field field,
			String keyField,
			String sourceLabel
	) {
		LinkedHashMap<String, JsonNode> indexed = new LinkedHashMap<>();
		for (int i = 0; i < entries.size(); i++) {
			JsonNode entry = entries.get(i);
			if (entry == null || !entry.isObject())
				throw new ConfigException("Field " + describe(field) + " expected object entries in " + sourceLabel + " list at index " + i);

			String key = extractKey((ObjectNode) entry, keyField, field, sourceLabel, i);
			if (indexed.containsKey(key))
				throw new ConfigException("Field " + describe(field) + " contains duplicate " + sourceLabel + " entry key '" + key + "'");

			indexed.put(key, entry.deepCopy());
		}

		return indexed;
	}

	private String extractKey(
			ObjectNode entry,
			String keyField,
			Field field,
			String sourceLabel,
			int index
	) {
		JsonNode keyNode = entry.get(keyField);
		if (keyNode == null || keyNode.isNull() || !keyNode.isValueNode() || keyNode.asText().isBlank())
			throw new ConfigException("Field " + describe(field) + " has a " + sourceLabel + " entry at index " + index + " without a valid key field '" + keyField + "'");
		return keyNode.asText();
	}

	private JsonNode mergeEntryDefaults(
			JsonNode genericDefaults,
			JsonNode keyedDefaultEntry,
			Class<?> entryType,
			MergeBehavior propertyBehavior,
			MergeContext.RecursiveMerge recursiveMerge
	) {
		if (genericDefaults == null)
			return keyedDefaultEntry == null || keyedDefaultEntry.isNull() ? null : keyedDefaultEntry.deepCopy();

		JsonNode base = genericDefaults.deepCopy();
		if (keyedDefaultEntry == null || keyedDefaultEntry.isNull())
			return base;
		if (!base.isObject() || !keyedDefaultEntry.isObject())
			return keyedDefaultEntry.deepCopy();

		return recursiveMerge.merge(keyedDefaultEntry, base, entryType, false, propertyBehavior);
	}

	private JsonNode sourceOwnsList(JsonNode source, JsonNode defaults) {
		if (source != null && !source.isNull())
			return source.deepCopy();
		if (defaults == null) return mapper.createArrayNode();
		return defaults.deepCopy();
	}

	private ConfigException wrongNodeType(Field field, JsonNode actual) {
		return new ConfigException("Field " + describe(field) + " expected a " + "list node but found " + actual.getNodeType());
	}

	private String describe(Field field) {
		return field.getDeclaringClass().getName() + "#" + field.getName();
	}
}
