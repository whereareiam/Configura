package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.MergeStrategy;
import me.whereareiam.configura.annotation.Policy;
import me.whereareiam.configura.exception.ConfigException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ConfigMerger {
	public static <T> ObjectNode buildMergedNodeFavorExisting(Path path, T model, ObjectMapper mapper) {
		ObjectNode modelNode = mapper.valueToTree(model);
		if (!Files.exists(path)) return modelNode;

		try {
			JsonNode existing = mapper.readTree(path.toFile());
			if (existing != null && existing.isObject()) {
				ObjectNode existingNode = (ObjectNode) existing;
				mergeExistingIntoModel(modelNode, existingNode, model.getClass());
			}
		} catch (Exception e) {
			throw new ConfigException("Failed to merge existing config with model: " + path, e);
		}

		return modelNode;
	}

	public static <T> ObjectNode buildMergedNodeFavorModel(Path path, T model, ObjectMapper mapper) {
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

	public static void mergeExistingIntoModel(ObjectNode modelNode, ObjectNode existingNode, Class<?> modelClass) {
		existingNode.fieldNames().forEachRemaining(key -> {
			JsonNode existingVal = existingNode.get(key);
			JsonNode modelVal = modelNode.get(key);

			// Check if this field has MAP_ADDITIVE_ONLY policy
			MergeStrategy strategy = getFieldMergeStrategy(modelClass, key);

			// For MAP_ADDITIVE_ONLY: if existing file has this field, replace model's value entirely
			// This prevents template keys from being merged in
			if (strategy == MergeStrategy.MAP_ADDITIVE_ONLY) {
				if (existingVal != null && !existingVal.isNull()) {
					modelNode.set(key, existingVal);
				}
				return;
			}

			// Default behavior: deep merge for objects
			if (existingVal != null && !existingVal.isNull() && existingVal.isObject() && modelVal != null && modelVal.isObject()) {
				// Get the actual field type for nested recursion
				Class<?> nestedClass = getFieldType(modelClass, key);
				mergeExistingIntoModel((ObjectNode) modelVal, (ObjectNode) existingVal, nestedClass != null ? nestedClass : modelClass);
				return;
			}

			if (existingVal != null && !existingVal.isNull())
				modelNode.set(key, existingVal);
		});
	}

	/**
	 * Gets the merge strategy for a specific field in a model class.
	 */
	private static MergeStrategy getFieldMergeStrategy(Class<?> modelClass, String fieldName) {
		try {
			Field field = modelClass.getDeclaredField(fieldName);
			Policy policy = field.getAnnotation(Policy.class);
			if (policy != null) {
				return policy.value();
			}
		} catch (NoSuchFieldException ignored) {
			// Field doesn't exist in Java class
		}
		return MergeStrategy.DEFAULT;
	}

	/**
	 * Gets the type of a specific field in a model class.
	 * Used for recursive merging to check nested field policies.
	 * For Map fields, returns the Map's value type (generic parameter) instead of Map.class.
	 */
	private static Class<?> getFieldType(Class<?> modelClass, String fieldName) {
		try {
			Field field = modelClass.getDeclaredField(fieldName);
			Class<?> fieldType = field.getType();
			
			// If field is a Map, extract the value type (V in Map<K, V>)
			if (Map.class.isAssignableFrom(fieldType)) {
				Type genericType = field.getGenericType();
				if (genericType instanceof ParameterizedType paramType) {
					Type[] typeArgs = paramType.getActualTypeArguments();
					// typeArgs[0] is key type, typeArgs[1] is value type
					if (typeArgs.length >= 2 && typeArgs[1] instanceof Class)
						return (Class<?>) typeArgs[1];
				}
			}
			
			return fieldType;
		} catch (NoSuchFieldException ignored) {
			// Field doesn't exist in Java class
			return null;
		}
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


