package me.whereareiam.configura.merge.plugin.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PropertyDefaultsResolver {
	public @Nullable JsonNode resolve(ObjectMapper mapper, Class<?> targetType, Field property) {
		JsonNode provider = resolveProvider(mapper, targetType, property);
		if (provider != null) return provider;

		JsonNode source = resolveSource(mapper, property);
		if (source != null) return source;

		JsonNode inlineLiteral = resolveInlineLiteral(mapper, property);
		if (inlineLiteral != null) return inlineLiteral;

		return resolveInlineObject(mapper, property);
	}

	public @Nullable JsonNode resolveInlineLiteral(ObjectMapper mapper, Field property) {
		Defaults defaults = property.getAnnotation(Defaults.class);
		if (defaults == null) return null;

		Object value;
		if (!defaults.text().isEmpty()) value = defaults.text();
		else if (!defaults.number().isEmpty()) value = parseNumber(defaults.number());
		else if (defaults.bool()) value = true;
		else value = parseLiteral(defaults.literal());

		return value == null ? null : mapper.valueToTree(value);
	}

	public @Nullable JsonNode resolveInlineObject(ObjectMapper mapper, Field property) {
		Defaults defaults = property.getAnnotation(Defaults.class);
		if (defaults == null) return null;
		if (defaults.properties().length > 0) return buildObject(mapper, defaults.properties());
		if (defaults.object().properties().length > 0) return buildObject(mapper, defaults.object().properties());
		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public @Nullable JsonNode resolveProvider(ObjectMapper mapper, Class<?> targetType, Field property) {
		Defaults defaults = property.getAnnotation(Defaults.class);
		if (defaults == null || defaults.provider().value() == Defaults.Provider.None.class) return null;

		DefaultsProvider provider = instantiateProvider(defaults.provider().value());
		Object instance = instantiateTarget(targetType);
		Object supplied = provider.supply(instance);
		return supplied == null ? null : mapper.valueToTree(supplied);
	}

	public @Nullable JsonNode resolveSource(ObjectMapper mapper, Field property) {
		Defaults defaults = property.getAnnotation(Defaults.class);
		if (defaults == null || defaults.source().value().isEmpty()) return null;
		return loadResource(mapper, defaults.source().value());
	}

	public static @Nullable Object parseLiteral(Defaults.Literal literal) {
		if (!literal.text().isEmpty()) return literal.text();
		if (!literal.number().isEmpty()) return parseNumber(literal.number());
		if (literal.bool()) return true;
		return null;
	}

	private static ObjectNode buildObject(ObjectMapper mapper, Defaults.Property[] properties) {
		ObjectNode node = mapper.createObjectNode();
		for (Defaults.Property property : properties) {
			Object value = !property.text().isEmpty() || !property.number().isEmpty() || property.bool()
					? parseLiteral(asLiteral(property.text(), property.number(), property.bool()))
					: parseLiteral(property.value());
			node.set(property.name(), mapper.valueToTree(value));
		}
		return node;
	}

	private static Defaults.Literal asLiteral(String text, String number, boolean bool) {
		return new Defaults.Literal() {
			@Override
			public String text() {
				return text;
			}

			@Override
			public String number() {
				return number;
			}

			@Override
			public boolean bool() {
				return bool;
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return Defaults.Literal.class;
			}
		};
	}

	private static @Nullable JsonNode loadResource(ObjectMapper mapper, String ref) {
		if (ref.startsWith("classpath:")) {
			String path = ref.substring("classpath:".length());
			try (var in = PropertyDefaultsResolver.class.getResourceAsStream(path)) {
				if (in == null) return null;
				try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
					return mapper.readTree(reader);
				}
			} catch (Exception ignored) {
				return null;
			}
		}

		if (ref.startsWith("file:")) {
			Path path = Path.of(ref.substring("file:".length()));
			try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				return mapper.readTree(reader);
			} catch (Exception ignored) {
				return null;
			}
		}

		return null;
	}

	private static DefaultsProvider<?> instantiateProvider(Class<? extends DefaultsProvider<?>> providerClass) {
		try {
			var constructor = providerClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate merge defaults provider: " + providerClass.getName(), e);
		}
	}

	private static Object instantiateTarget(Class<?> targetType) {
		try {
			var constructor = targetType.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate defaults target: " + targetType.getName(), e);
		}
	}

	private static @Nullable Object parseNumber(String number) {
		try {
			return Long.parseLong(number);
		} catch (NumberFormatException e) {
			try {
				return Double.parseDouble(number);
			} catch (NumberFormatException ignored) {
				return number;
			}
		}
	}
}
