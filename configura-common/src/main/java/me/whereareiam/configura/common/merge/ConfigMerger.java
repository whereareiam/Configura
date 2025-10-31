package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Policy;
import me.whereareiam.configura.exception.ConfigException;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

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
				overlayModelOverExisting(modelNode, existingNode, model.getClass());
			}
		} catch (Exception e) {
			throw new ConfigException("Failed to merge (model-wins) with existing config: " + path, e);
		}

		return modelNode;
	}

	public static void mergeExistingIntoModel(ObjectNode modelNode, ObjectNode existingNode, Class<?> modelClass) {
		preservePolicyFields(existingNode, modelNode, modelClass);

		existingNode.fieldNames().forEachRemaining(key -> {
			JsonNode existingVal = existingNode.get(key);
			JsonNode modelVal = modelNode.get(key);

			Policy policy = getFieldPolicy(modelClass, key);
			boolean preserveWrite = policy != null && policy.preserveWrite();

			if (preserveWrite) {
				if (existingVal != null && !existingVal.isNull()) modelNode.set(key, existingVal);
				return;
			}

			if (existingVal != null && !existingVal.isNull() && existingVal.isObject() && modelVal != null && modelVal.isObject()) {
				mergeExistingIntoModel((ObjectNode) modelVal, (ObjectNode) existingVal, modelClass);
				return;
			}

			if (existingVal != null && !existingVal.isNull())
				modelNode.set(key, existingVal);
		});

		for (Field f : modelClass.getDeclaredFields()) {
			Policy p = f.getAnnotation(Policy.class);
			if (p == null || !p.skipMerge()) continue;

			String key = f.getName();
			JsonNode existingVal = existingNode.get(key);
			boolean hasNonNullExisting = existingVal != null && !existingVal.isNull();

			if (!hasNonNullExisting && modelNode.has(key))
				modelNode.remove(key);
		}
	}

	public static void preservePolicyFields(ObjectNode existingNode, ObjectNode modelNode, Class<?> modelClass) {
		for (Field field : modelClass.getDeclaredFields()) {
			Policy policy = field.getAnnotation(Policy.class);

			if (policy != null && !policy.mergeOnUpdate()) {
				String key = field.getName();
				JsonNode existingVal = existingNode.get(key);

				if (existingVal != null && !existingVal.isNull())
					modelNode.set(key, existingVal);
			}
		}
	}

	private static Policy getFieldPolicy(Class<?> modelClass, String key) {
		try {
			Field f = modelClass.getDeclaredField(key);

			return f.getAnnotation(Policy.class);
		} catch (NoSuchFieldException ignored) {
			return null;
		}
	}

	private static void overlayModelOverExisting(ObjectNode modelNode, ObjectNode existingNode, Class<?> modelClass) {
		existingNode.fieldNames().forEachRemaining(key -> {
			JsonNode existingVal = existingNode.get(key);
			JsonNode modelVal = modelNode.get(key);

			Policy policy = getFieldPolicy(modelClass, key);
			boolean preserveWrite = policy != null && (policy.preserveWrite() || !policy.mergeOnUpdate());

			if (preserveWrite && existingVal != null && !existingVal.isNull()) {
				modelNode.set(key, existingVal);
				return;
			}

			if (existingVal != null && !existingVal.isNull() && modelVal != null && modelVal.isObject() && existingVal.isObject())
				overlayModelOverExisting((ObjectNode) modelVal, (ObjectNode) existingVal, modelClass);
		});
	}
}


