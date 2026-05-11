package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public final class MergeFieldResolver {
	public static Field resolveField(Class<?> ownerType, String fieldName) {
		for (Field field : ownerType.getDeclaredFields()) {
			if (field.getName().equals(fieldName)) return field;
			if (resolveFieldName(field).equals(fieldName)) return field;
		}

		return null;
	}

	public static String resolveFieldName(Field field) {
		JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
		if (jsonProperty != null && jsonProperty.value() != null && !jsonProperty.value().isBlank())
			return jsonProperty.value();

		return field.getName();
	}

	public static Class<?> resolveChildType(Field field, Class<?> fallback) {
		if (field == null) return fallback;
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
}
