package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.annotation.PreserveUnknownFields;
import me.whereareiam.configura.common.merge.defaults.ModelDefaults;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.document.DocumentProcessor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Merges what a user wrote with the defaults of the model, key by key, following each field's
 * {@link FieldRule}. The user's values always win; the rules only decide what is added around them.
 */
@RequiredArgsConstructor
final class TreeMerge {
	private final ObjectMapper mapper;
	private final DocumentProcessor documents;
	private final ModelDefaults modelDefaults;
	private final PrimitiveDefaultPolicy defaultsPolicy;

	/**
	 * Merges two objects bound to a model type.
	 *
	 * @param source       what the file has, if anything
	 * @param defaults     what the model has by default, if anything
	 * @param ownerType    model type the keys belong to
	 * @param keepsUnknown whether keys the model does not know are kept
	 */
	ObjectNode object(@Nullable JsonNode source, @Nullable JsonNode defaults, Class<?> ownerType, boolean keepsUnknown) {
		ObjectNode result = mapper.createObjectNode();
		ObjectNode sourceObject = source instanceof ObjectNode object ? object : mapper.createObjectNode();
		ObjectNode defaultObject = defaults instanceof ObjectNode object ? object : mapper.createObjectNode();
		ObjectNode current = sourceObject.isEmpty() ? defaultObject : sourceObject;

		Set<String> keys = new LinkedHashSet<>();
		sourceObject.fieldNames().forEachRemaining(keys::add);
		defaultObject.fieldNames().forEachRemaining(keys::add);

		for (String key : keys) {
			JsonNode sourceValue = sourceObject.get(key);
			JsonNode defaultValue = defaultObject.get(key);
			Field field = SerializedFieldResolver.resolveField(ownerType, key);
			if (sourceValue != null && sourceValue.isNull()) {
				result.set(key, sourceValue);
				continue;
			}

			if (defaultValue == null) {
				if (field != null || keepsUnknown) result.set(key, sourceValue.deepCopy());
				continue;
			}

			DocumentTypeContext context = new DocumentTypeContext(
					sourceValue != null ? sourceValue : defaultValue,
					current,
					field,
					key,
					null,
					null
			);
			Class<?> valueType = documents.resolveType(SerializedFieldResolver.valueType(ownerType, field), context);
			boolean valueKeepsUnknown = keepsUnknown || keepsUnknown(field) || keepsUnknown(valueType);

			JsonNode merged = field(FieldRule.of(field), field, sourceValue, defaultValue, valueType, valueKeepsUnknown);
			if (merged != null && !merged.isNull()) result.set(key, merged);
		}

		return result;
	}

	static boolean keepsUnknown(@Nullable Class<?> type) {
		return type != null && type.isAnnotationPresent(PreserveUnknownFields.class);
	}

	private static boolean keepsUnknown(@Nullable Field field) {
		return field != null && field.isAnnotationPresent(PreserveUnknownFields.class);
	}

	private @Nullable JsonNode field(
			FieldRule rule,
			@Nullable Field field,
			@Nullable JsonNode source,
			JsonNode defaults,
			Class<?> valueType,
			boolean keepsUnknown
	) {
		if (source == null && !rule.addsDefaultWhenAbsent()) return null;
		if (source != null && !rule.fillsMissing()) return source.deepCopy();

		if (rule.isMap()) return map(rule, field, source, defaults, valueType, keepsUnknown);
		if (rule.isKeyedList()) return keyedList(rule, field, source, defaults, valueType, keepsUnknown);
		if (source == null) return defaults.deepCopy();
		if (source.isObject() && defaults.isObject()) return object(source, defaults, valueType, keepsUnknown);

		return source.deepCopy();
	}

	private ObjectNode map(
			FieldRule rule,
			Field field,
			@Nullable JsonNode source,
			JsonNode defaults,
			Class<?> valueType,
			boolean keepsUnknown
	) {
		if (source != null && !source.isObject()) throw wrongNode(field, "a map", source);
		if (!defaults.isObject()) throw wrongNode(field, "a map", defaults);

		Map<String, JsonNode> sourceEntries = new LinkedHashMap<>();
		if (source != null) source.properties().forEach(entry -> sourceEntries.put(entry.getKey(), entry.getValue()));

		Map<String, JsonNode> defaultEntries = new LinkedHashMap<>();
		defaults.properties().forEach(entry -> defaultEntries.put(entry.getKey(), entry.getValue()));

		ObjectNode result = mapper.createObjectNode();
		entries(rule, field, sourceEntries, defaultEntries, source == null, valueType, keepsUnknown, false)
				.forEach(result::set);

		return result;
	}

	private ArrayNode keyedList(
			FieldRule rule,
			Field field,
			@Nullable JsonNode source,
			JsonNode defaults,
			Class<?> entryType,
			boolean keepsUnknown
	) {
		if (source != null && !source.isArray()) throw wrongNode(field, "a list", source);
		if (!defaults.isArray()) throw wrongNode(field, "a list", defaults);

		Map<String, JsonNode> sourceEntries = source == null ? new LinkedHashMap<>() : index(rule, field, source, "file");
		Map<String, JsonNode> defaultEntries = index(rule, field, defaults, "default");

		ArrayNode result = mapper.createArrayNode();
		entries(rule, field, sourceEntries, defaultEntries, source == null, entryType, keepsUnknown, true)
				.values()
				.forEach(result::add);

		return result;
	}

	/**
	 * Merges the entries of a map or keyed list: the user's entries first, each completed from the
	 * default entry of the same key and the defaults of the entry type, then the default entries the
	 * user does not have, if the rule adds them or the user has no collection at all.
	 */
	private Map<String, JsonNode> entries(
			FieldRule rule,
			Field field,
			Map<String, JsonNode> sourceEntries,
			Map<String, JsonNode> defaultEntries,
			boolean sourceAbsent,
			Class<?> entryType,
			boolean keepsUnknown,
			boolean list
	) {
		Map<String, JsonNode> result = new LinkedHashMap<>();
		for (Map.Entry<String, JsonNode> entry : sourceEntries.entrySet()) {
			JsonNode defaultEntry = defaultEntries.get(entry.getKey());
			if (defaultEntry == null && rule.rejectsUnknownEntries())
				throw new ConfigException("Field " + describe(field) + " does not allow the entry '" + entry.getKey() + "'");

			result.put(entry.getKey(), entry(field, entry.getKey(), entry.getValue(), defaultEntry, entryType, keepsUnknown, list));
		}

		if (!rule.addsMissingEntries() && !sourceAbsent) return result;

		for (Map.Entry<String, JsonNode> entry : defaultEntries.entrySet())
			if (!sourceEntries.containsKey(entry.getKey()))
				result.put(entry.getKey(), entry(field, entry.getKey(), null, entry.getValue(), entryType, keepsUnknown, list));

		return result;
	}

	private JsonNode entry(
			Field field,
			String key,
			@Nullable JsonNode source,
			@Nullable JsonNode keyedDefault,
			Class<?> declaredType,
			boolean keepsUnknown,
			boolean list
	) {
		DocumentTypeContext context = new DocumentTypeContext(
				source != null ? source : keyedDefault,
				null,
				field,
				field.getName(),
				list ? null : key,
				list ? key : null
		);
		Class<?> entryType = documents.resolveType(declaredType, context);
		JsonNode defaults = entryDefaults(modelDefaults.ofType(entryType, defaultsPolicy, context), keyedDefault, entryType, keepsUnknown);

		if (list && !(defaults instanceof ObjectNode))
			throw new ConfigException("Field " + describe(field) + " is merged by key, so its entries must be objects");

		if (defaults == null) return source == null ? mapper.nullNode() : source.deepCopy();
		if (defaults.isObject()) return object(source, defaults, entryType, keepsUnknown);

		return source != null && !source.isNull() ? source.deepCopy() : defaults.deepCopy();
	}

	/** Lays the default entry of a key over the defaults every entry of that type has. */
	private @Nullable JsonNode entryDefaults(
			@Nullable JsonNode typeDefaults,
			@Nullable JsonNode keyedDefault,
			Class<?> entryType,
			boolean keepsUnknown
	) {
		boolean keyed = keyedDefault != null && !keyedDefault.isNull();
		if (typeDefaults == null) return keyed ? keyedDefault.deepCopy() : null;
		if (!keyed) return typeDefaults.deepCopy();
		if (!typeDefaults.isObject() || !keyedDefault.isObject()) return keyedDefault.deepCopy();

		return object(keyedDefault, typeDefaults, entryType, keepsUnknown);
	}

	private Map<String, JsonNode> index(FieldRule rule, Field field, JsonNode list, String origin) {
		Map<String, JsonNode> indexed = new LinkedHashMap<>();
		for (int position = 0; position < list.size(); position++) {
			JsonNode entry = list.get(position);
			if (entry == null || !entry.isObject())
				throw new ConfigException("Field " + describe(field) + " is merged by key, but entry " + position + " of the " + origin + " list is not an object");

			JsonNode key = entry.get(rule.key());
			if (key == null || !key.isValueNode() || key.isNull() || key.asText().isBlank())
				throw new ConfigException("Field " + describe(field) + " has no '" + rule.key() + "' in entry " + position + " of the " + origin + " list");

			if (indexed.put(key.asText(), entry) != null)
				throw new ConfigException("Field " + describe(field) + " has the entry '" + key.asText() + "' twice in the " + origin + " list");
		}

		return indexed;
	}

	private static ConfigException wrongNode(Field field, String expected, JsonNode actual) {
		return new ConfigException("Field " + describe(field) + " expects " + expected + " but found " + actual.getNodeType());
	}

	private static String describe(Field field) {
		return field.getDeclaringClass().getName() + "#" + field.getName();
	}
}
