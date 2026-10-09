package me.whereareiam.configura.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Relates the keys of a document to the fields of the model it is bound to.
 */
public final class SerializedFieldResolver {
	private SerializedFieldResolver() {
	}

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

	/**
	 * Returns the type of the values a key holds: the element type of a list, the value type of a
	 * map, the type of any other field. A key without a field is taken to hold its owner's type.
	 */
	public static Class<?> valueType(Class<?> ownerType, @Nullable Field field) {
		if (field == null) return ownerType;
		if (List.class.isAssignableFrom(field.getType())) return typeArgument(field, 0);
		if (Map.class.isAssignableFrom(field.getType())) return typeArgument(field, 1);

		return field.getType();
	}

	private static Class<?> typeArgument(Field field, int index) {
		if (!(field.getGenericType() instanceof ParameterizedType parameterized)) return field.getType();

		Type[] arguments = parameterized.getActualTypeArguments();
		if (index >= arguments.length) return field.getType();

		Type argument = arguments[index];
		if (argument instanceof Class<?> type) return type;
		if (argument instanceof ParameterizedType nested && nested.getRawType() instanceof Class<?> raw) return raw;

		return field.getType();
	}
}
