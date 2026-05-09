package me.whereareiam.configura.common.polymorphic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public final class PolymorphicDefinition {
	private final String discriminator;
	private final Map<String, Class<?>> mappings;
	private final String defaultValue;
	private final LinkedHashMap<String, Class<?>> inferFields;
	private final Class<?> defaultTarget;
}
