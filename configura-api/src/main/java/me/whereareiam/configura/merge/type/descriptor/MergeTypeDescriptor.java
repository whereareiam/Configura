package me.whereareiam.configura.merge.type.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Describes one serialized field considered by the merge engine.
 */
public final class MergeTypeDescriptor {
	private final @NotNull Class<?> ownerType;
	private final @Nullable Field field;
	private final @NotNull String serializedName;
	private final @NotNull Class<?> declaredType;
	private final @NotNull Type genericType;

	public MergeTypeDescriptor(
			@NotNull Class<?> ownerType,
			@Nullable Field field,
			@NotNull String serializedName,
			@NotNull Class<?> declaredType,
			@NotNull Type genericType
	) {
		this.ownerType = ownerType;
		this.field = field;
		this.serializedName = serializedName;
		this.declaredType = declaredType;
		this.genericType = genericType;
	}

	public @NotNull Class<?> getOwnerType() {
		return ownerType;
	}

	public @Nullable Field getField() {
		return field;
	}

	public @NotNull String getSerializedName() {
		return serializedName;
	}

	public @NotNull Class<?> getDeclaredType() {
		return declaredType;
	}

	public @NotNull Type getGenericType() {
		return genericType;
	}

	public @Nullable Class<?> resolveGenericArgument(int index) {
		if (!(genericType instanceof ParameterizedType parameterizedType))
			return null;
		Type[] arguments = parameterizedType.getActualTypeArguments();
		if (index < 0 || index >= arguments.length || !(arguments[index] instanceof Class<?> argumentType))
			return null;
		return argumentType;
	}
}
