package me.whereareiam.configura.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.whereareiam.configura.common.polymorphic.PolymorphicModule;
import me.whereareiam.configura.type.Format;

import java.util.Objects;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class MapperFactory {
	private static final ConcurrentHashMap<Key, ObjectMapper> CACHE = new ConcurrentHashMap<>();

	public static ObjectMapper buildWriterMapper(Format format) {
        return buildReaderMapper(format);
	}

	public static ObjectMapper buildReaderMapper(Format format) {
		Key key = new Key(format);
		return CACHE.computeIfAbsent(key, k -> createBase(format));
	}

	private static ObjectMapper createBase(Format format) {
		return switch (format) {
			case YAML -> createYamlMapper();
			case JSON -> createJsonMapper();
		};
	}

	private static ObjectMapper createYamlMapper() {
		YAMLFactory factory = new YAMLFactory();
		factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
		factory.disable(YAMLGenerator.Feature.SPLIT_LINES);
		factory.enable(YAMLGenerator.Feature.INDENT_ARRAYS);
		factory.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);

		ObjectMapper mapper = new ObjectMapper(factory);

		return configureCommon(mapper);
	}

	public static ObjectMapper createYamlMapper(List<Module> modules) {
		YAMLFactory factory = new YAMLFactory();
		factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
		factory.disable(YAMLGenerator.Feature.SPLIT_LINES);
		factory.enable(YAMLGenerator.Feature.INDENT_ARRAYS);
		factory.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);

		ObjectMapper mapper = new ObjectMapper(factory);
		return configureModern(mapper, modules);
	}

	private static ObjectMapper createJsonMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter()
				.withSeparators(Separators.createDefaultInstance()
						.withObjectFieldValueSpacing(Separators.Spacing.AFTER));
		prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
		mapper.setDefaultPrettyPrinter(prettyPrinter);

		return configureCommon(mapper);
	}

	public static ObjectMapper createJsonMapper(List<Module> modules) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter()
				.withSeparators(Separators.createDefaultInstance()
						.withObjectFieldValueSpacing(Separators.Spacing.AFTER));
		prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
		mapper.setDefaultPrettyPrinter(prettyPrinter);

		return configureModern(mapper, modules);
	}

	private static ObjectMapper configureCommon(ObjectMapper mapper) {
		mapper.registerModule(new JavaTimeModule());
		mapper.registerModule(new PolymorphicModule());

		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
		mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));

		return mapper;
	}

	private static ObjectMapper configureModern(ObjectMapper mapper, List<Module> modules) {
		mapper.registerModule(new JavaTimeModule());
		mapper.registerModule(new PolymorphicModule());

		if (modules != null) {
			for (Module module : modules) {
				if (module != null) mapper.registerModule(module);
			}
		}

		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
		mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));

		return mapper;
	}

	private record Key(Format format) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && format == k.format;
		}

		@Override
		public int hashCode() {
			return Objects.hash(format);
		}
	}
}
