package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.MergeEngine;
import me.whereareiam.configura.common.merge.MergeFieldResolver;
import me.whereareiam.configura.common.merge.defaults.type.ClassProviderDefaultsResolver;
import me.whereareiam.configura.common.util.PathNavigator;

import java.lang.reflect.Field;
import java.util.List;

public final class MergeDefaultsResolver {
	private final ObjectMapper mapper;
	private final DefaultMergeDefaultsRegistry defaultsRegistry;
	private final List<FieldDefaultsResolver> fieldResolvers;

	public MergeDefaultsResolver(ObjectMapper mapper, DefaultMergeDefaultsRegistry defaultsRegistry) {
		this.mapper = mapper;
		this.defaultsRegistry = defaultsRegistry;
		this.fieldResolvers = FieldDefaultsResolver.standard(defaultsRegistry);
	}

	public <T> ObjectNode resolve(T model, Class<T> type, MergeEngine.Mode mode) {
		ObjectNode node = mapper.valueToTree(model);
		applyClassDefaults(node, type, mode);
		applyFieldDefaults(node, type, mode);
		return node;
	}

	private void applyClassDefaults(ObjectNode target, Class<?> type, MergeEngine.Mode mode) {
		JsonNode classDefaults = new ClassProviderDefaultsResolver(defaultsRegistry).resolve(mapper, type);
		if (classDefaults == null || !classDefaults.isObject()) return;
		deepFill(target, (ObjectNode) classDefaults, type, mode);
	}

	private void applyFieldDefaults(ObjectNode node, Class<?> type, MergeEngine.Mode mode) {
		for (Field field : type.getDeclaredFields()) {
			String key = MergeFieldResolver.resolveFieldName(field);
			JsonNode defaultValue = resolveFieldDefault(field);
			if (defaultValue != null) {
				JsonNode existing = node.get(key);
				if (isMissing(existing, mode)) {
					node.set(key, defaultValue.deepCopy());
					continue;
				}
				if (existing.isObject() && defaultValue.isObject())
					deepFill((ObjectNode) existing, (ObjectNode) defaultValue, MergeFieldResolver.resolveChildType(field, type), mode);
				continue;
			}

			Class<?> childType = MergeFieldResolver.resolveChildType(field, type);
			if (hasFieldDefaults(childType)) {
				JsonNode existing = node.get(key);
				ObjectNode child = existing != null && existing.isObject() ? (ObjectNode) existing : mapper.createObjectNode();
				PathNavigator.write(node, key, child);
				applyFieldDefaults(child, childType, mode);
			}
		}
	}

	private JsonNode resolveFieldDefault(Field field) {
		for (FieldDefaultsResolver resolver : fieldResolvers) {
			JsonNode value = resolver.resolve(mapper, field.getType(), field);
			if (value != null) return value;
		}

		return null;
	}

	private boolean hasFieldDefaults(Class<?> type) {
		for (Field field : type.getDeclaredFields()) {
			if (field.getAnnotation(Defaults.class) != null) return true;
		}

		return false;
	}

	private void deepFill(ObjectNode target, ObjectNode defaults, Class<?> type, MergeEngine.Mode mode) {
		for (var entry : defaults.properties()) {
			String key = entry.getKey();
			JsonNode existing = target.get(key);
			JsonNode value = entry.getValue();
			if (isMissing(existing, mode)) {
				target.set(key, value.deepCopy());
				continue;
			}
			if (existing.isObject() && value.isObject()) {
				Field field = MergeFieldResolver.resolveField(type, key);
				deepFill((ObjectNode) existing, (ObjectNode) value, MergeFieldResolver.resolveChildType(field, type), mode);
			}
		}
	}

	private boolean isMissing(JsonNode node, MergeEngine.Mode mode) {
		if (node == null || node.isNull()) return true;
		if (mode != MergeEngine.Mode.DEFAULT_INSTANCE) return false;
		return (node.isNumber() && node.asDouble() == 0.0)
				|| (node.isBoolean() && !node.asBoolean())
				|| (node.isArray() && node.isEmpty());
	}
}
