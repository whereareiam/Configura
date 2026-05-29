package me.whereareiam.configura.merge.plugin.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Describes one serialized field considered by the merge engine.
 */
public final class MergeDescriptor {
	private final @NotNull Class<?> ownerType;
	private final @Nullable Field field;
	private final @NotNull String serializedName;
	private final @NotNull Class<?> declaredType;
	private final @NotNull Type genericType;

	/**
	 * Creates a field descriptor.
	 *
	 * @param ownerType class that owns the field
	 * @param field reflected field, or {@code null} when the serialized key is not declared
	 * @param serializedName serialized property name
	 * @param declaredType declared field type, or a fallback type when the field is unknown
	 * @param genericType generic field type, or the declared type when generic metadata is unavailable
	 */
	public MergeDescriptor(
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

	/**
	 * Returns the class that owns the field.
	 *
	 * @return owning class
	 */
	public @NotNull Class<?> getOwnerType() {
		return ownerType;
	}

	/**
	 * Returns the reflected field when the serialized key maps to a declared field.
	 *
	 * @return reflected field or {@code null}
	 */
	public @Nullable Field getField() {
		return field;
	}

	/**
	 * Returns the serialized field name.
	 *
	 * @return serialized name
	 */
	public @NotNull String getSerializedName() {
		return serializedName;
	}

	/**
	 * Returns the declared field type.
	 *
	 * @return declared type
	 */
	public @NotNull Class<?> getDeclaredType() {
		return declaredType;
	}

	/**
	 * Returns the generic field type.
	 *
	 * @return generic type metadata
	 */
	public @NotNull Type getGenericType() {
		return genericType;
	}
}
