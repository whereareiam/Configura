package me.whereareiam.configura.merge.plugin.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.type.BuiltinStrategyCapabilities;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.type.merge.tree.map.MapPresence;
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
			BuiltinStrategyCapabilities.MapCapability strategyCapability,
			MergeBehavior propertyBehavior,
			MapMergeConfig config,
			MergeTypeAdapterContext context
	) {
		BuiltinStrategyCapabilities.MapCapability.Mode strategyMode = strategyCapability == null
				? null
				: strategyCapability.mode();
		if (strategyMode == BuiltinStrategyCapabilities.MapCapability.Mode.SOURCE_OWNS) return sourceOwnsMap(source, defaults);
		if (strategyMode == BuiltinStrategyCapabilities.MapCapability.Mode.NEVER_DEFAULTS) return source == null ? mapper.nullNode() : source.deepCopy();
		if (strategyMode != BuiltinStrategyCapabilities.MapCapability.Mode.DEEP_DEFAULTS)
			throw unsupportedStrategy(property, context);

		if (source != null && !source.isNull() && !source.isObject()) throw wrongNodeType(property, source);
		if (defaults != null && !defaults.isNull() && !defaults.isObject()) throw wrongNodeType(property, defaults);

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

			DocumentTypeContext entryContext = documentContext(property, key, sourceEntry, defaultEntry);
			Class<?> effectiveValueType = context.resolveDocumentType(valueType, entryContext);
			JsonNode mergedDefaults = mergeEntryDefaults(
					context.resolveModelDefaults(effectiveValueType, entryContext),
					defaultEntry,
					effectiveValueType,
					propertyBehavior,
					context.getRecursiveMerge()
			);
			JsonNode merged = mergeEntry(sourceEntry, mergedDefaults, effectiveValueType, propertyBehavior, context.getRecursiveMerge());
			result.set(key, merged);
		});

		if (seedsDefaultEntries(config) || source == null || source.isNull()) {
			defaultObject.fieldNames().forEachRemaining(key -> {
				if (seen.contains(key)) return;
				DocumentTypeContext entryContext = documentContext(property, key, null, defaultObject.get(key));
				Class<?> effectiveValueType = context.resolveDocumentType(valueType, entryContext);
				JsonNode mergedDefaults = mergeEntryDefaults(
						context.resolveModelDefaults(effectiveValueType, entryContext),
						defaultObject.get(key),
						effectiveValueType,
						propertyBehavior,
						context.getRecursiveMerge()
				);
				result.set(key, mergeEntry(null, mergedDefaults, effectiveValueType, propertyBehavior, context.getRecursiveMerge()));
			});
		}

		return result;
	}

	private boolean seedsDefaultEntries(MapMergeConfig config) {
		return config.getPresence() == MapPresence.SEED_DEFAULTS || config.getPresence() == MapPresence.DEFAULT_DOMAIN_ONLY;
	}

	private JsonNode mergeEntry(
			JsonNode sourceEntry,
			JsonNode defaultEntry,
			Class<?> valueType,
			MergeBehavior propertyBehavior,
			MergeContext.RecursiveMerge recursiveMerge
	) {
		if (defaultEntry == null) return sourceEntry == null ? mapper.nullNode() : sourceEntry.deepCopy();
		if (defaultEntry.isObject()) return recursiveMerge.merge(sourceEntry, defaultEntry, valueType, false, propertyBehavior);
		if (sourceEntry != null && !sourceEntry.isNull()) return sourceEntry.deepCopy();

		return defaultEntry.deepCopy();
	}

	private JsonNode mergeEntryDefaults(
			JsonNode genericDefaults,
			JsonNode keyedDefaultEntry,
			Class<?> valueType,
			MergeBehavior propertyBehavior,
			MergeContext.RecursiveMerge recursiveMerge
	) {
		if (genericDefaults == null) {
			return keyedDefaultEntry == null || keyedDefaultEntry.isNull() ? null : keyedDefaultEntry.deepCopy();
		}

		JsonNode base = genericDefaults.deepCopy();
		if (keyedDefaultEntry == null || keyedDefaultEntry.isNull()) return base;
		if (!base.isObject() || !keyedDefaultEntry.isObject()) return keyedDefaultEntry.deepCopy();

		return recursiveMerge.merge(keyedDefaultEntry, base, valueType, false, propertyBehavior);
	}

	private JsonNode sourceOwnsMap(JsonNode source, JsonNode defaults) {
		if (source != null && !source.isNull()) return source.deepCopy();
		if (defaults == null) return mapper.createObjectNode();

		return defaults.deepCopy();
	}

	private ConfigException wrongNodeType(Field field, JsonNode actual) {
		return new ConfigException("Field " + describe(field) + " expected a map/object node but found " + actual.getNodeType());
	}

	private ConfigException unsupportedStrategy(Field field, MergeTypeAdapterContext context) {
		String strategyName = context.getStrategy() == null
				? "<none>"
				: context.getStrategy().getClass().getName();
		return new ConfigException("Field " + describe(field) + " uses unsupported merge strategy " + strategyName + " with @MergeMap");
	}

	private String describe(Field field) {
		return field.getDeclaringClass().getName() + "#" + field.getName();
	}

	private DocumentTypeContext documentContext(
			Field field,
			String mapKey,
			JsonNode sourceEntry,
			JsonNode defaultEntry
	) {
		return new DocumentTypeContext(
				sourceEntry != null ? sourceEntry : defaultEntry,
				null,
				field,
				field.getName(),
				mapKey,
				null
		);
	}
}
