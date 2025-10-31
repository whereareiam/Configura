package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BytesSerializationTest {
	static class Model {
		@Field
		public String name;

		@Field
		public int port;
	}

	@Test
	void toBytes_and_fromBytes_roundtrip() {
		Model m = new Model();
		m.name = "service";
		m.port = 8080;

		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		byte[] bytes = writer.encode(m);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		Model restored = reader.load(bytes, Model.class);

		assertEquals("service", restored.name);
		assertEquals(8080, restored.port);
	}

	@Test
	void fromBytes_with_empty_bytes_returns_default_instance() {
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		Model restored = reader.load(new byte[0], Model.class);
		assertNotNull(restored);
		// Defaults: String null, int 0
		assertNull(restored.name);
		assertEquals(0, restored.port);
	}
}


