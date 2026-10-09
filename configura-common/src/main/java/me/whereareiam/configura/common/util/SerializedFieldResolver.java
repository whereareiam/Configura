package me.whereareiam.configura.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class SerializedFieldResolver {
	/**
	 * Lists the fields a model serializes: those of the class and of its superclasses, the class's
	 * own first.
	 */
	public static List<Field> fields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass())
			for (Field field : current.getDeclaredFields()) {
				int modifiers = field.getModifiers();
				if (!field.isSynthetic() && !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers))
					fields.add(field);
			}

		return fields;
	}

	public static @Nullable Field resolveField(Class<?> ownerType, String serializedName) {
		for (Field field : fields(ownerType)) {
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
