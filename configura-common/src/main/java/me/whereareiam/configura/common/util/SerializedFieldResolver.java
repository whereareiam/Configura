package me.whereareiam.configura.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public final class SerializedFieldResolver {
	public static @Nullable Field resolveField(Class<?> ownerType, String serializedName) {
		for (Field field : ownerType.getDeclaredFields()) {
			if (field.getName().equals(serializedName)) return field;
			if (resolveSerializedName(field).equals(serializedName)) return field;
		}

		return null;
	}

	public static String resolveSerializedName(Field field) {
		JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
		if (jsonProperty != null && jsonProperty.value() != null && !jsonProperty.value().isBlank())
			return jsonProperty.value();

		return field.getName();
	}

	public static Class<?> resolveDeclaredType(@Nullable Field field, Class<?> fallback) {
		if (field == null) return fallback;
		return field.getType();
	}
}
