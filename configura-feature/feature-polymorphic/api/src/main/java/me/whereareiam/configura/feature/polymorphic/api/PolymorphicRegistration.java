package me.whereareiam.configura.feature.polymorphic.api;

public interface PolymorphicRegistration<T> {
	PolymorphicRegistration<T> discriminator(String name);

	PolymorphicRegistration<T> map(String value, Class<? extends T> target);

	PolymorphicRegistration<T> defaultValue(String value);

	PolymorphicRegistration<T> inferByField(String fieldName, Class<? extends T> target);

	PolymorphicRegistration<T> defaultTarget(Class<? extends T> target);

	void build();
}
