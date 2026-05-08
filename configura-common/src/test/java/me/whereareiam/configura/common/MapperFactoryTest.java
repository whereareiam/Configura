package me.whereareiam.configura.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class MapperFactoryTest {
	@Test
	void cachesByFormat() {
		ObjectMapper m1 = MapperFactory.buildWriterMapper(Format.YAML);
		ObjectMapper m2 = MapperFactory.buildWriterMapper(Format.YAML);
		ObjectMapper m3 = MapperFactory.buildWriterMapper(Format.JSON);
		assertSame(m1, m2);
		assertNotSame(m1, m3);
	}
}

