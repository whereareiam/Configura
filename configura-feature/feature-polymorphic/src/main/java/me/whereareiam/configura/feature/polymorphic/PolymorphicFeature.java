package me.whereareiam.configura.feature.polymorphic;

import com.fasterxml.jackson.databind.Module;
import me.whereareiam.configura.ConfiguraFeature;
import me.whereareiam.configura.document.DocumentTypeResolver;
import me.whereareiam.configura.feature.polymorphic.api.PolymorphicRegistration;
import me.whereareiam.configura.feature.polymorphic.api.PolymorphicRegistry;
import me.whereareiam.configura.feature.polymorphic.module.PolymorphicSerializationModule;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PolymorphicFeature implements ConfiguraFeature, PolymorphicRegistry {
	private final DefaultPolymorphicRegistry registry = new DefaultPolymorphicRegistry();

	public static @NotNull PolymorphicFeature defaults() {
		return new PolymorphicFeature();
	}

	@Override
	public <T> @NotNull PolymorphicRegistration<T> register(@NotNull Class<T> baseType) {
		return registry.register(baseType);
	}

	@Override
	public Object definition(@NotNull Class<?> baseType) {
		return registry.definition(baseType);
	}

	@Override
	public @NotNull List<Module> modules(@NotNull com.fasterxml.jackson.databind.ObjectMapper plainMapper) {
		return List.of(new PolymorphicSerializationModule(registry));
	}

	@Override
	public @NotNull List<DocumentTypeResolver> typeResolvers() {
		return List.of(new PolymorphicTypeResolver(registry));
	}
}
