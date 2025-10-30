package me.whereareiam.configura.builder;

public interface PolymorphicBuilder<T> {
	PolymorphicBuilder<T> discriminator(String name);

	PolymorphicBuilder<T> map(String value, Class<? extends T> target);

	PolymorphicBuilder<T> defaultValue(String value);

	PolymorphicBuilder<T> inferByField(String fieldName, Class<? extends T> target);

	PolymorphicBuilder<T> defaultTarget(Class<? extends T> target);

	void build();
}