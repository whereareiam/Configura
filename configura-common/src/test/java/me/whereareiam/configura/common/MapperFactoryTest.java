package me.whereareiam.configura.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.common.adapter.AdapterRegistry;
import me.whereareiam.configura.common.template.DefaultTemplateRegistry;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class MapperFactoryTest {
	static class T {
	}

	public static class TA implements TypeAdapter<T> {
		public T deserialize(String value) {
			return new T();
		}

		public String serialize(T value) {
			return "x";
		}
	}

	@Test
	void cachesByFormatAndRegistry() {
		AdapterRegistry r1 = AdapterRegistry.empty();
		AdapterRegistry r2 = r1.withAdapter(T.class, TA.class);
		DefaultTemplateRegistry tr = new DefaultTemplateRegistry();
		ObjectMapper m1 = MapperFactory.buildWriterMapper(Format.YAML, r1);
		ObjectMapper m2 = MapperFactory.buildWriterMapper(Format.YAML, r1);
		ObjectMapper m3 = MapperFactory.buildWriterMapper(Format.YAML, r2);
		assertSame(m1, m2);
		assertNotSame(m1, m3);
	}
}


