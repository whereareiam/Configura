package me.whereareiam.configura.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.annotation.merge.*;
import me.whereareiam.configura.merge.defaults.context.DefaultsContext;
import me.whereareiam.configura.merge.defaults.descriptor.DefaultsDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Built-in field defaults resolver for Configura's annotation DSL.
 */
public final class AnnotationMergeDefaultsResolver implements DefaultsResolver {
	@Override
	public @Nullable JsonNode resolve(@NotNull DefaultsDescriptor descriptor, @NotNull DefaultsContext context) {
		Field field = descriptor.getField();
		if (field == null) return null;

		ObjectMapper mapper = context.getMapper();

		JsonNode source = resolveSource(mapper, field);
		if (source != null) return source;

		JsonNode provider = resolveProvider(mapper, descriptor.getDeclaredType(), field);
		if (provider != null) return provider;

		JsonNode value = resolveValue(mapper, field);
		if (value != null) return value;

		JsonNode object = resolveObject(mapper, field);
		if (object != null) return object;

		JsonNode map = resolveMap(mapper, field);
		if (map != null) return map;

		return resolveList(mapper, field);
	}

	private @Nullable JsonNode resolveValue(ObjectMapper mapper, Field field) {
		MergeValue value = field.getAnnotation(MergeValue.class);
		if (value != null) {
			Object literal = literal(value.text(), value.number(), value.bool());
			return literal == null ? null : mapper.valueToTree(literal);
		}

		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null) return null;
		if (!defaults.text().isEmpty()) return mapper.valueToTree(defaults.text());
		if (!defaults.number().isEmpty()) return mapper.valueToTree(parseNumber(defaults.number()));
		if (defaults.bool()) return mapper.valueToTree(true);
		if (!defaults.literal().text().isEmpty()) return mapper.valueToTree(defaults.literal().text());
		if (!defaults.literal().number().isEmpty()) return mapper.valueToTree(parseNumber(defaults.literal().number()));
		if (defaults.literal().bool()) return mapper.valueToTree(true);

		return null;
	}

	private @Nullable JsonNode resolveObject(ObjectMapper mapper, Field field) {
		MergeObject object = field.getAnnotation(MergeObject.class);
		if (object != null && object.properties().length > 0)
			return buildObject(mapper, object.properties());

		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null) return null;
		if (defaults.properties().length > 0) return buildObject(mapper, defaults.properties());
		if (defaults.object().properties().length > 0) return buildObject(mapper, defaults.object().properties());

		return null;
	}

	private @Nullable JsonNode resolveMap(ObjectMapper mapper, Field field) {
		MergeMap map = field.getAnnotation(MergeMap.class);
		if (map != null && map.entries().length > 0)
			return buildMap(mapper, map.entries());

		return null;
	}

	private @Nullable JsonNode resolveList(ObjectMapper mapper, Field field) {
		MergeList list = field.getAnnotation(MergeList.class);
		if (list != null && list.items().length > 0)
			return buildArray(mapper, list.items());

		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null) return null;
		if (defaults.stringItems().length > 0) {
			ArrayNode array = mapper.createArrayNode();
			for (String value : defaults.stringItems())
				array.add(value);

			return array;
		}

		if (defaults.items().length > 0) return buildArray(mapper, defaults.items());
		if (defaults.list().items().length > 0) return buildArray(mapper, defaults.list().items());

		return null;
	}

	private @Nullable JsonNode resolveSource(ObjectMapper mapper, Field field) {
		MergeDefaultsSource source = field.getAnnotation(MergeDefaultsSource.class);
		if (source != null) return loadResource(mapper, source.value());

		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null || defaults.source().value().isEmpty()) return null;
		return loadResource(mapper, defaults.source().value());
	}

	@SuppressWarnings({"rawtypes"})
	private @Nullable JsonNode resolveProvider(ObjectMapper mapper, Class<?> targetType, Field field) {
		me.whereareiam.configura.annotation.merge.DefaultsProvider provider =
				field.getAnnotation(me.whereareiam.configura.annotation.merge.DefaultsProvider.class);

		if (provider != null) {
			me.whereareiam.configura.merge.defaults.DefaultsProvider instance = instantiateProvider(provider.value());
			return supplied(mapper, targetType, instance);
		}

		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null || defaults.provider().value() == Defaults.Provider.None.class) return null;
		me.whereareiam.configura.merge.defaults.DefaultsProvider instance = instantiateProvider(defaults.provider().value());
		return supplied(mapper, targetType, instance);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private @Nullable JsonNode supplied(
			ObjectMapper mapper,
			Class<?> targetType,
			me.whereareiam.configura.merge.defaults.DefaultsProvider provider
	) {
		Object target = instantiateTarget(targetType);
		Object supplied = provider.supply(target);
		return supplied == null ? null : mapper.valueToTree(supplied);
	}

	private static @Nullable Object literal(String text, String number, boolean bool) {
		if (!text.isEmpty()) return text;
		if (!number.isEmpty()) return parseNumber(number);
		if (bool) return true;
		return null;
	}

	private static ArrayNode buildArray(ObjectMapper mapper, MergeList.Item[] items) {
		ArrayNode array = mapper.createArrayNode();
		for (MergeList.Item item : items) {
			Object literal = literal(item.text(), item.number(), item.bool());
			if (literal != null) array.add(mapper.valueToTree(literal));
			else array.add(buildObject(mapper, item.properties()));
		}
		return array;
	}

	private static ArrayNode buildArray(ObjectMapper mapper, Defaults.Literal[] items) {
		ArrayNode array = mapper.createArrayNode();
		for (Defaults.Literal item : items) {
			Object literal = literal(item.text(), item.number(), item.bool());
			if (literal != null) array.add(mapper.valueToTree(literal));
		}
		return array;
	}

	private static ObjectNode buildMap(ObjectMapper mapper, MergeMap.Entry[] entries) {
		ObjectNode object = mapper.createObjectNode();
		for (MergeMap.Entry entry : entries) {
			Object literal = literal(entry.text(), entry.number(), entry.bool());
			JsonNode value = literal != null ? mapper.valueToTree(literal) : buildObject(mapper, entry.properties());
			object.set(entry.key(), value);
		}
		return object;
	}

	private static ObjectNode buildObject(ObjectMapper mapper, MergeObject.Property[] properties) {
		ObjectNode object = mapper.createObjectNode();
		for (MergeObject.Property property : properties) {
			Object literal = literal(property.text(), property.number(), property.bool());
			object.set(property.name(), mapper.valueToTree(literal));
		}
		return object;
	}

	private static ObjectNode buildObject(ObjectMapper mapper, Defaults.Property[] properties) {
		ObjectNode object = mapper.createObjectNode();
		for (Defaults.Property property : properties) {
			Object literal = literal(property.text(), property.number(), property.bool());
			if (literal == null)
				literal = literal(property.value().text(), property.value().number(), property.value().bool());
			object.set(property.name(), mapper.valueToTree(literal));
		}
		return object;
	}

	private static @Nullable JsonNode loadResource(ObjectMapper mapper, String ref) {
		if (ref.startsWith("classpath:")) {
			String path = ref.substring("classpath:".length());
			try (var in = AnnotationMergeDefaultsResolver.class.getResourceAsStream(path)) {
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

	private static me.whereareiam.configura.merge.defaults.DefaultsProvider<?> instantiateProvider(
			Class<? extends me.whereareiam.configura.merge.defaults.DefaultsProvider<?>> providerClass
	) {
		try {
			var constructor = providerClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception exception) {
			throw new IllegalStateException("Cannot instantiate merge defaults provider: " + providerClass.getName(), exception);
		}
	}

	private static Object instantiateTarget(Class<?> targetType) {
		try {
			var constructor = targetType.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception exception) {
			throw new IllegalStateException("Cannot instantiate defaults target: " + targetType.getName(), exception);
		}
	}

	private static Object parseNumber(String number) {
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
