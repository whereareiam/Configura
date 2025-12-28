package me.whereareiam.configura.common.polymorphic;

import me.whereareiam.configura.builder.PolymorphicBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PolymorphicRegistry {
	private static final ConcurrentHashMap<Class<?>, PolymorphicInfo> REGISTRY = new ConcurrentHashMap<>();

	public static <T> Builder<T> register(Class<T> baseType) {
		return new Builder<>(baseType);
	}

	public static PolymorphicInfo get(Class<?> baseType) {
		return REGISTRY.get(baseType);
	}

	public static final class Builder<T> implements PolymorphicBuilder<T> {
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
		public Builder<T> discriminator(String name) {
			this.discriminator = name;
			return this;
		}

		@Override
		public Builder<T> map(String value, Class<? extends T> target) {
			this.mappings.put(value, target);
			return this;
		}

		@Override
		public Builder<T> defaultValue(String v) {
			this.defaultValue = v;
			return this;
		}

		@Override
		public Builder<T> inferByField(String fieldName, Class<? extends T> target) {
			this.inferFields.put(fieldName, target);
			return this;
		}

		@Override
		public Builder<T> defaultTarget(Class<? extends T> target) {
			this.defaultTarget = target;
			return this;
		}

		@Override
		public void build() {
			// Merge with existing registration if present
			PolymorphicInfo existing = REGISTRY.get(baseType);
			if (existing == null) {
				REGISTRY.put(baseType, new PolymorphicInfo(discriminator, Map.copyOf(mappings), defaultValue,
						new LinkedHashMap<>(inferFields), defaultTarget));
				return;
			}

			// Merge inferFields (new fields take precedence if conflict)
			LinkedHashMap<String, Class<?>> mergedInferFields = new LinkedHashMap<>(existing.getInferFields());
			mergedInferFields.putAll(inferFields);

			// Merge mappings (new mappings take precedence if conflict)
			Map<String, Class<?>> mergedMappings = new LinkedHashMap<>(existing.getMappings());
			mergedMappings.putAll(mappings);

			// Use new discriminator/defaultValue/defaultTarget only if explicitly set
			String finalDiscriminator = discriminator != null ? discriminator : existing.getDiscriminator();
			String finalDefaultValue = defaultValue != null ? defaultValue : existing.getDefaultValue();
			Class<?> finalDefaultTarget = defaultTarget != null ? defaultTarget : existing.getDefaultTarget();

			REGISTRY.put(baseType, new PolymorphicInfo(finalDiscriminator, Map.copyOf(mergedMappings),
					finalDefaultValue, mergedInferFields, finalDefaultTarget));
		}
	}
}


