package me.whereareiam.configura.feature.polymorphic.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PolymorphicRegistry {
	<T> @NotNull PolymorphicRegistration<T> register(@NotNull Class<T> baseType);

	@Nullable Object definition(@NotNull Class<?> baseType);
}
