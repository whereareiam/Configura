package me.whereareiam.configura.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.common.adapter.AdapterModule;
import me.whereareiam.configura.common.adapter.AdapterRegistry;
import me.whereareiam.configura.common.polymorphic.PolymorphicModule;
import me.whereareiam.configura.common.template.TemplateModule;
import me.whereareiam.configura.type.Format;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class MapperFactory {
	private static final ConcurrentHashMap<Key, ObjectMapper> CACHE = new ConcurrentHashMap<>();

	public static ObjectMapper buildMapper(Format format, AdapterRegistry registry) {
		Key key = new Key(format, registry);
		return CACHE.computeIfAbsent(key, k -> create(format, registry.instantiate()));
	}

	private static ObjectMapper create(Format format, Map<Class<?>, TypeAdapter<?>> adapters) {
		ObjectMapper mapper = format == Format.YAML ? new ObjectMapper(new YAMLFactory()) : new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.registerModule(new AdapterModule(adapters));
		mapper.registerModule(new TemplateModule(adapters));
		mapper.registerModule(new PolymorphicModule());

		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));

		return mapper;
	}

	private record Key(Format format, AdapterRegistry registry) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && format == k.format && Objects.equals(registry, k.registry);
		}

		@Override
		public int hashCode() {
			return Objects.hash(format, registry);
		}
	}
}



