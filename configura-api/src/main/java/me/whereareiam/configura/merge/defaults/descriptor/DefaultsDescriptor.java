package me.whereareiam.configura.merge.defaults.descriptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Describes a field or type during defaults resolution.
 */
public final class DefaultsDescriptor {
	private final @NotNull ObjectMapper mapper;
	private final @NotNull Class<?> ownerType;
	private final @Nullable Field field;
	private final @NotNull String serializedName;
	private final @NotNull Class<?> declaredType;
	private final @NotNull Type genericType;

	public DefaultsDescriptor(
			@NotNull ObjectMapper mapper,
			@NotNull Class<?> ownerType,
			@Nullable Field field,
			@NotNull String serializedName,
			@NotNull Class<?> declaredType,
			@NotNull Type genericType
	) {
		this.mapper = mapper;
		this.ownerType = ownerType;
		this.field = field;
		this.serializedName = serializedName;
		this.declaredType = declaredType;
		this.genericType = genericType;
	}

	public @NotNull ObjectMapper getMapper() {
		return mapper;
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
}
