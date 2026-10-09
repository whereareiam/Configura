package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.merge.MergeDefaultsSource;
import me.whereareiam.configura.annotation.merge.MergeList;
import me.whereareiam.configura.annotation.merge.MergeMap;
import me.whereareiam.configura.annotation.merge.MergeObject;
import me.whereareiam.configura.annotation.merge.MergeValue;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.merge.defaults.DefaultsResolver;
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
 * Reads the default of a field from the merge annotations on it: a resource, a provider, a literal
 * value, or the properties, entries or items of an object, map or list.
 */
public final class AnnotationDefaultsResolver implements DefaultsResolver {
	private static final String CLASSPATH_PREFIX = "classpath:";
	private static final String FILE_PREFIX = "file:";

	@Override
	public @Nullable JsonNode resolve(@NotNull DefaultsDescriptor descriptor, @NotNull DefaultsContext context) {
		Field field = descriptor.getField();
		if (field == null) return null;

		ObjectMapper mapper = context.getMapper();
		JsonNode source = resolveSource(mapper, field);
		if (source != null) return source;

		JsonNode provided = resolveProvider(mapper, descriptor.getDeclaredType(), field);
		if (provided != null) return provided;

		JsonNode value = resolveValue(mapper, field);
		if (value != null) return value;

		JsonNode object = resolveObject(mapper, field);
		if (object != null) return object;

		JsonNode map = resolveMap(mapper, field);
		if (map != null) return map;

		return resolveList(mapper, field);
	}

	private @Nullable JsonNode resolveSource(ObjectMapper mapper, Field field) {
		MergeDefaultsSource source = field.getAnnotation(MergeDefaultsSource.class);

		return source == null ? null : loadResource(mapper, source.value());
	}

	private @Nullable JsonNode resolveProvider(ObjectMapper mapper, Class<?> targetType, Field field) {
		me.whereareiam.configura.annotation.merge.DefaultsProvider provider =
				field.getAnnotation(me.whereareiam.configura.annotation.merge.DefaultsProvider.class);
		if (provider == null) return null;

		Object supplied = supply(instantiate(provider.value()), instantiate(targetType));
		return supplied == null ? null : mapper.valueToTree(supplied);
	}

	private @Nullable JsonNode resolveValue(ObjectMapper mapper, Field field) {
		MergeValue value = field.getAnnotation(MergeValue.class);
		if (value == null) return null;

		Object literal = literal(value.text(), value.number(), value.bool());
		return literal == null ? null : mapper.valueToTree(literal);
	}

	private @Nullable JsonNode resolveObject(ObjectMapper mapper, Field field) {
		MergeObject object = field.getAnnotation(MergeObject.class);

		return object == null || object.properties().length == 0 ? null : buildObject(mapper, object.properties());
	}

	private @Nullable JsonNode resolveMap(ObjectMapper mapper, Field field) {
		MergeMap map = field.getAnnotation(MergeMap.class);
		if (map == null || map.entries().length == 0) return null;

		ObjectNode object = mapper.createObjectNode();
		for (MergeMap.Entry entry : map.entries()) {
			Object literal = literal(entry.text(), entry.number(), entry.bool());
			object.set(entry.key(), literal != null ? mapper.valueToTree(literal) : buildObject(mapper, entry.properties()));
		}

		return object;
	}

	private @Nullable JsonNode resolveList(ObjectMapper mapper, Field field) {
		MergeList list = field.getAnnotation(MergeList.class);
		if (list == null || list.items().length == 0) return null;

		ArrayNode array = mapper.createArrayNode();
		for (MergeList.Item item : list.items()) {
			Object literal = literal(item.text(), item.number(), item.bool());
			array.add(literal != null ? mapper.valueToTree(literal) : buildObject(mapper, item.properties()));
		}

		return array;
	}

	private static ObjectNode buildObject(ObjectMapper mapper, MergeObject.Property[] properties) {
		ObjectNode object = mapper.createObjectNode();
		for (MergeObject.Property property : properties)
			object.set(property.name(), mapper.valueToTree(literal(property.text(), property.number(), property.bool())));

		return object;
	}

	private static @Nullable Object literal(String text, String number, boolean bool) {
		if (!text.isEmpty()) return text;
		if (!number.isEmpty()) return parseNumber(number);

		return bool ? true : null;
	}

	private static Object parseNumber(String number) {
		try {
			return Long.parseLong(number);
		} catch (NumberFormatException notWhole) {
			try {
				return Double.parseDouble(number);
			} catch (NumberFormatException notANumber) {
				return number;
			}
		}
	}

	/** Loads defaults from a {@code classpath:} or {@code file:} reference; an unreadable one yields no default. */
	private static @Nullable JsonNode loadResource(ObjectMapper mapper, String reference) {
		try {
			if (reference.startsWith(CLASSPATH_PREFIX)) {
				try (var input = AnnotationDefaultsResolver.class.getResourceAsStream(reference.substring(CLASSPATH_PREFIX.length()))) {
					if (input == null) return null;

					return mapper.readTree(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)));
				}
			}

			if (reference.startsWith(FILE_PREFIX)) {
				try (var reader = Files.newBufferedReader(Path.of(reference.substring(FILE_PREFIX.length())), StandardCharsets.UTF_8)) {
					return mapper.readTree(reader);
				}
			}
		} catch (Exception unreadable) {
			return null;
		}

		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static @Nullable Object supply(DefaultsProvider provider, Object target) {
		return provider.supply(target);
	}

	private static <T> T instantiate(Class<T> type) {
		try {
			var constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);

			return constructor.newInstance();
		} catch (Exception failure) {
			throw new IllegalStateException("Cannot instantiate " + type.getName() + " for merge defaults", failure);
		}
	}
}
