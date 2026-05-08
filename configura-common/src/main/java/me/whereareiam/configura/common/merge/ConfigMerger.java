package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergePolicy;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ConfigMerger {
	public static <T> ObjectNode buildMergedNodeFavorExisting(Path path, T model, ObjectMapper mapper) {
		return buildMergedNodeFavorExisting(path, model, mapper, new MergePolicyResolver());
	}

	public static <T> ObjectNode buildMergedNodeFavorExisting(Path path, T model, ObjectMapper mapper, MergePolicyResolver resolver) {
		ObjectNode modelNode = mapper.valueToTree(model);
		if (!Files.exists(path)) {
			mergeExistingIntoModel(modelNode, mapper.createObjectNode(), model.getClass(), resolver);
			return modelNode;
		}

		try {
			JsonNode existing = mapper.readTree(path.toFile());
			if (existing != null && existing.isObject()) {
				ObjectNode existingNode = (ObjectNode) existing;
				mergeExistingIntoModel(modelNode, existingNode, model.getClass(), resolver);
			}
		} catch (Exception e) {
			throw new ConfigException("Failed to merge existing config with model: " + path, e);
		}

		return modelNode;
	}

	public static <T> ObjectNode buildMergedNodeFavorModel(Path path, T model, ObjectMapper mapper) {
		return buildMergedNodeFavorModel(path, model, mapper, new MergePolicyResolver());
	}

	public static <T> ObjectNode buildMergedNodeFavorModel(Path path, T model, ObjectMapper mapper, MergePolicyResolver resolver) {
		ObjectNode modelNode = mapper.valueToTree(model);
		if (!Files.exists(path)) return modelNode;

		try {
			JsonNode existing = mapper.readTree(path.toFile());
			if (existing != null && existing.isObject()) {
				ObjectNode existingNode = (ObjectNode) existing;
				overlayModelOverExisting(modelNode, existingNode);
			}
		} catch (Exception e) {
			throw new ConfigException("Failed to merge (model-wins) with existing config: " + path, e);
		}

		return modelNode;
	}

	public static void mergeExistingIntoModel(ObjectNode modelNode, ObjectNode existingNode, Class<?> modelClass, MergePolicyResolver resolver) {
		applyPresentKeyStrategies(modelNode, existingNode, modelClass, resolver);
		pruneNestedPresentKeyStrategies(modelNode, existingNode, modelClass, resolver);

		existingNode.fieldNames().forEachRemaining(key -> {
			JsonNode existingVal = existingNode.get(key);
			JsonNode modelVal = modelNode.get(key);

			MergePolicy policy = resolver.resolve(modelClass, key);
			if (existingVal != null && existingVal.isNull() && policy.preserveExplicitNull()) {
				modelNode.set(key, existingVal);
				return;
			}

			if (policy.mapMode() == MergePolicy.MapMode.DECLARED_SOURCE_KEYS_ONLY) return;
			if (policy.valueMode() == MergePolicy.ValueMode.SOURCE_OWNS_VALUE
					|| policy.objectMode() == MergePolicy.ObjectMode.SOURCE_OWNS_OBJECT
					|| policy.mapMode() == MergePolicy.MapMode.SOURCE_OWNS_MAP
					|| policy.listMode() == MergePolicy.ListMode.SOURCE_OWNS_LIST) {
				if (existingVal != null && !existingVal.isNull()) {
					modelNode.set(key, existingVal);
				}
				return;
			}

			if (policy.valueMode() == MergePolicy.ValueMode.NEVER_TEMPLATE) {
				if (existingVal != null && !existingVal.isNull()) {
					modelNode.set(key, existingVal);
				} else if (existingVal == null) {
					modelNode.remove(key);
				}
				return;
			}

			if (existingVal != null && !existingVal.isNull() && existingVal.isObject() && modelVal != null && modelVal.isObject()) {
				Class<?> nestedClass = getFieldType(modelClass, key);
				mergeExistingIntoModel((ObjectNode) modelVal, (ObjectNode) existingVal, nestedClass != null ? nestedClass : modelClass, resolver);
				return;
			}

			if (existingVal != null && !existingVal.isNull())
				modelNode.set(key, existingVal);
		});
	}

	private static void applyPresentKeyStrategies(ObjectNode modelNode, ObjectNode existingNode, Class<?> modelClass, MergePolicyResolver resolver) {
		for (Field field : modelClass.getDeclaredFields()) {
			if (resolver.resolve(field).mapMode() != MergePolicy.MapMode.DECLARED_SOURCE_KEYS_ONLY) continue;

			String key = MergePolicyResolver.resolveFieldName(field);
			if (key == null || key.isBlank()) continue;

			JsonNode existingVal = existingNode.get(key);
			JsonNode modelVal = modelNode.get(key);
			if (existingVal == null) {
				modelNode.remove(key);
				continue;
			}
			if (existingVal.isNull() || modelVal == null || !modelVal.isObject() || !existingVal.isObject()) {
				modelNode.set(key, existingVal);
				continue;
			}

			Class<?> nestedClass = getFieldType(modelClass, key);
			ObjectNode merged = mergePresentKeys((ObjectNode) modelVal, (ObjectNode) existingVal, nestedClass != null ? nestedClass : modelClass, resolver);
			modelNode.set(key, merged);
		}
	}

	private static ObjectNode mergePresentKeys(ObjectNode modelNode, ObjectNode existingNode, Class<?> modelClass, MergePolicyResolver resolver) {
		ObjectNode result = modelNode.objectNode();
		existingNode.fieldNames().forEachRemaining(key -> {
			JsonNode existingVal = existingNode.get(key);
			JsonNode modelVal = modelNode.get(key);

			if (existingVal == null) return;
			if (existingVal.isNull()) {
				result.set(key, existingVal);
				return;
			}

			if (existingVal.isObject() && modelVal != null && modelVal.isObject()) {
				ObjectNode copy = modelVal.deepCopy();
				Class<?> nestedClass = getFieldType(modelClass, key);
				mergeExistingIntoModel(copy, (ObjectNode) existingVal, nestedClass != null ? nestedClass : modelClass, resolver);
				result.set(key, copy);
				return;
			}

			result.set(key, existingVal);
		});
		return result;
	}

	private static void pruneNestedPresentKeyStrategies(ObjectNode modelNode, ObjectNode existingNode, Class<?> modelClass, MergePolicyResolver resolver) {
		for (Field field : modelClass.getDeclaredFields()) {
			if (resolver.resolve(field).mapMode() == MergePolicy.MapMode.DECLARED_SOURCE_KEYS_ONLY)
				continue;

			String key = MergePolicyResolver.resolveFieldName(field);
			JsonNode modelVal = modelNode.get(key);
			if (!(modelVal instanceof ObjectNode modelObject)) continue;

			Class<?> rawType = field.getType();
			if (Map.class.isAssignableFrom(rawType)) continue;

			JsonNode existingVal = existingNode.get(key);
			ObjectNode existingObject = existingVal instanceof ObjectNode objectNode
					? objectNode
					: modelNode.objectNode();
			pruneNestedPresentKeyStrategies(modelObject, existingObject, rawType, resolver);
		}
	}

	private static Class<?> getFieldType(Class<?> modelClass, String fieldName) {
		Field field = resolveField(modelClass, fieldName);
		if (field == null) return null;

		Class<?> fieldType = field.getType();
		if (Map.class.isAssignableFrom(fieldType)) {
			Type genericType = field.getGenericType();
			if (genericType instanceof ParameterizedType paramType) {
				Type[] typeArgs = paramType.getActualTypeArguments();
				if (typeArgs.length >= 2 && typeArgs[1] instanceof Class)
					return (Class<?>) typeArgs[1];
			}
		}

		return fieldType;
	}

	private static Field resolveField(Class<?> modelClass, String fieldName) {
		for (Field field : modelClass.getDeclaredFields()) {
			if (field.getName().equals(fieldName)) return field;
			if (MergePolicyResolver.resolveFieldName(field).equals(fieldName)) return field;
		}

		return null;
	}

	private static void overlayModelOverExisting(ObjectNode modelNode, ObjectNode existingNode) {
		existingNode.fieldNames().forEachRemaining(key -> {
			JsonNode existingVal = existingNode.get(key);
			JsonNode modelVal = modelNode.get(key);

			if (existingVal != null && !existingVal.isNull() && modelVal != null && modelVal.isObject() && existingVal.isObject())
				overlayModelOverExisting((ObjectNode) modelVal, (ObjectNode) existingVal);
		});
	}
}
