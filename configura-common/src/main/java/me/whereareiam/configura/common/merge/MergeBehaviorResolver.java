package me.whereareiam.configura.common.merge;

import me.whereareiam.configura.annotation.PreserveUnknownFields;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.type.UnknownFieldPolicy;

import java.lang.reflect.Field;

public final class MergeBehaviorResolver {
	public MergeBehavior resolve(MergeBehavior base, Class<?> type) {
		if (preservesUnknownFields(type))
			return preservingUnknownFields(base);

		return base;
	}

	public MergeBehavior resolve(MergeBehavior base, Field field, Class<?> childType) {
		if (preservesUnknownFields(field) || preservesUnknownFields(childType))
			return preservingUnknownFields(base);

		return base;
	}

	private static MergeBehavior preservingUnknownFields(MergeBehavior base) {
		return MergeBehavior.builder()
				.primitiveDefaults(base.getPrimitiveDefaultPolicy())
				.unknownFields(UnknownFieldPolicy.PRESERVE)
				.build();
	}

	private static boolean preservesUnknownFields(Class<?> type) {
		return type != null && type.isAnnotationPresent(PreserveUnknownFields.class);
	}

	private static boolean preservesUnknownFields(Field field) {
		return field != null && field.isAnnotationPresent(PreserveUnknownFields.class);
	}
}
