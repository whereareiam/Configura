package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.merge.defaults.DefaultsResolver;
import me.whereareiam.configura.merge.defaults.DefaultsResolverRegistry;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.MergeTypeAdapterRegistry;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestMergeProperties {
	public static @NotNull MergeTypeAdapterRegistry adapterRegistry() {
		return new MergeTypeAdapterRegistry().register(new TestPropertyTypeAdapter());
	}

	public static @NotNull DefaultsResolverRegistry defaultsResolverRegistry() {
		return new DefaultsResolverRegistry().register(new TestDefaultsResolver());
	}

	public static @NotNull MergePolicyResolverRegistry policyResolverRegistry() {
		return new MergePolicyResolverRegistry();
	}

	private static final class TestPropertyTypeAdapter implements MergeTypeAdapter {
		@Override
		public boolean supports(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
			return true;
		}

		@Override
		public @NotNull Class<?> resolveChildType(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
			Field field = descriptor.getField();
			if (field == null) return descriptor.getDeclaredType();
			if (java.util.Collection.class.isAssignableFrom(field.getType())) return resolveCollectionEntryType(field, descriptor.getDeclaredType());
			return descriptor.getDeclaredType();
		}

		@Override
		public @NotNull JsonNode merge(@NotNull MergeTypeAdapterContext context) {
			JsonNode source = context.getSourceNode();
			JsonNode defaults = context.getDefaultNode();
			if (source == null || source.isNull() || context.sourceTreatsDefaultAsMissing())
				return defaults == null ? context.getMapper().nullNode() : defaults.deepCopy();
			if (source.isObject() && defaults != null && defaults.isObject())
				return context.mergeChildren(source, defaults);
			return source.deepCopy();
		}

		private @NotNull Class<?> resolveCollectionEntryType(Field field, Class<?> fallback) {
			Type genericType = field.getGenericType();
			if (!(genericType instanceof ParameterizedType parameterizedType))
				return fallback;
			Type[] arguments = parameterizedType.getActualTypeArguments();
			if (arguments.length < 1 || !(arguments[0] instanceof Class<?> entryType))
				return fallback;
			return entryType;
		}
	}

	private static final class TestDefaultsResolver implements DefaultsResolver {
		@Override
		public @Nullable JsonNode resolve(
				@NotNull me.whereareiam.configura.merge.defaults.descriptor.DefaultsDescriptor descriptor,
				@NotNull me.whereareiam.configura.merge.defaults.context.DefaultsContext context
		) {
			Field field = descriptor.getField();
			if (field == null) return null;
			return resolveDefaults(context.getMapper(), descriptor.getDeclaredType(), field);
		}
	}

	private static @Nullable JsonNode resolveDefaults(ObjectMapper mapper, Class<?> targetType, Field property) {
		Defaults defaults = property.getAnnotation(Defaults.class);
		if (defaults == null) return null;

		if (defaults.items().length > 0) return buildArray(mapper, defaults.items());
		if (defaults.stringItems().length > 0) {
			ArrayNode array = mapper.createArrayNode();
			for (String value : defaults.stringItems())
				array.add(value);
			return array;
		}
		if (defaults.list().items().length > 0)
			return buildArray(mapper, defaults.list().items());

		JsonNode provider = resolveProvider(mapper, targetType, defaults);
		if (provider != null) return provider;

		JsonNode source = resolveSource(mapper, defaults);
		if (source != null) return source;

		Object inlineLiteral = parseLiteral(defaults.text(), defaults.number(), defaults.bool(), defaults.literal());
		if (inlineLiteral != null) return mapper.valueToTree(inlineLiteral);

		if (defaults.properties().length > 0) return buildObject(mapper, defaults.properties());
		if (defaults.object().properties().length > 0) return buildObject(mapper, defaults.object().properties());

		return null;
	}

	private static @Nullable JsonNode resolveProvider(ObjectMapper mapper, Class<?> targetType, Defaults defaults) {
		if (defaults.provider().value() == Defaults.Provider.None.class) return null;
		try {
			DefaultsProvider<?> provider = defaults.provider().value().getDeclaredConstructor().newInstance();
			Object instance = targetType.getDeclaredConstructor().newInstance();
			@SuppressWarnings("rawtypes")
			Object supplied = ((DefaultsProvider) provider).supply(instance);
			return supplied == null ? null : mapper.valueToTree(supplied);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate merge defaults provider: " + defaults.provider().value().getName(), e);
		}
	}

	private static @Nullable JsonNode resolveSource(ObjectMapper mapper, Defaults defaults) {
		String ref = defaults.source().value();
		if (ref.isEmpty()) return null;
		if (ref.startsWith("classpath:")) {
			String path = ref.substring("classpath:".length());
			try (var in = TestMergeProperties.class.getResourceAsStream(path)) {
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

	private static @Nullable Object parseLiteral(String text, String number, boolean bool, Defaults.Literal literal) {
		if (!text.isEmpty()) return text;
		if (!number.isEmpty()) return parseNumber(number);
		if (bool) return true;
		if (!literal.text().isEmpty()) return literal.text();
		if (!literal.number().isEmpty()) return parseNumber(literal.number());
		if (literal.bool()) return true;
		return null;
	}

	private static @NotNull ArrayNode buildArray(ObjectMapper mapper, Defaults.Literal[] literals) {
		ArrayNode array = mapper.createArrayNode();
		for (Defaults.Literal literal : literals) {
			Object value = parseLiteral("", "", false, literal);
			if (value != null) array.add(mapper.valueToTree(value));
		}
		return array;
	}

	private static @NotNull ObjectNode buildObject(ObjectMapper mapper, Defaults.Property[] properties) {
		ObjectNode node = mapper.createObjectNode();
		for (Defaults.Property property : properties) {
			Object value = !property.text().isEmpty() || !property.number().isEmpty() || property.bool()
					? parseLiteral("", "", false, parsePropertyLiteral(property.text(), property.number(), property.bool()))
					: parseLiteral("", "", false, property.value());
			node.set(property.name(), mapper.valueToTree(value));
		}
		return node;
	}

	private static Defaults.Literal parsePropertyLiteral(String text, String number, boolean bool) {
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
