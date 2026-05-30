package me.whereareiam.configura.feature.polymorphic;

import me.whereareiam.configura.feature.polymorphic.api.PolymorphicRegistration;
import me.whereareiam.configura.feature.polymorphic.api.PolymorphicRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultPolymorphicRegistry implements PolymorphicRegistry {
	private final ConcurrentHashMap<Class<?>, me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition> registry = new ConcurrentHashMap<>();

	@Override
	public <T> @NotNull PolymorphicRegistration<T> register(@NotNull Class<T> baseType) {
		return new Builder<>(baseType);
	}

	@Override
	public @Nullable me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition definition(@NotNull Class<?> baseType) {
		return registry.get(baseType);
	}

	private final class Builder<T> implements PolymorphicRegistration<T> {
		private final Class<T> baseType;
		private String discriminator;
		private final Map<String, Class<?>> mappings = new LinkedHashMap<>();
		private String defaultValue = "";
		private final LinkedHashMap<String, Class<?>> inferFields = new LinkedHashMap<>();
		private Class<?> defaultTarget;

		private Builder(Class<T> baseType) {
			this.baseType = baseType;
		}

		@Override
		public PolymorphicRegistration<T> discriminator(String name) {
			this.discriminator = name;
			return this;
		}

		@Override
		public PolymorphicRegistration<T> map(String value, Class<? extends T> target) {
			this.mappings.put(value, target);
			return this;
		}

		@Override
		public PolymorphicRegistration<T> defaultValue(String value) {
			this.defaultValue = value;
			return this;
		}

		@Override
		public PolymorphicRegistration<T> inferByField(String fieldName, Class<? extends T> target) {
			this.inferFields.put(fieldName, target);
			return this;
		}

		@Override
		public PolymorphicRegistration<T> defaultTarget(Class<? extends T> target) {
			this.defaultTarget = target;
			return this;
		}

		@Override
		public void build() {
			registry.put(
					baseType,
					new me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition(discriminator, Map.copyOf(mappings), defaultValue, new LinkedHashMap<>(inferFields), defaultTarget)
			);
		}
	}
}
