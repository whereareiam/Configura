package me.whereareiam.configura.common;

import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigIoRoundtripTest {
	static class AppConfig {
		public String name;
		public int port;
	}

	@Test
	void roundtripsYamlAndJsonFiles(@TempDir Path dir) {
		AppConfig cfg = new AppConfig();
		cfg.name = "svc";
		cfg.port = 8080;

		String yaml = dir.resolve("app.yml").toString();
		String json = dir.resolve("app.json").toString();

		ConfigWriter yamlWriter = new DefaultConfigWriter().withFormat(Format.YAML);
		ConfigWriter jsonWriter = new DefaultConfigWriter().withFormat(Format.JSON);
		yamlWriter.encode(yaml, cfg);
		jsonWriter.encode(json, cfg);

		ConfigReader yamlReader = new DefaultConfigReader().withFormat(Format.YAML);
		ConfigReader jsonReader = new DefaultConfigReader().withFormat(Format.JSON);
		AppConfig y = yamlReader.load(yaml, AppConfig.class);
		AppConfig j = jsonReader.load(json, AppConfig.class);

		assertEquals("svc", y.name);
		assertEquals(8080, y.port);
		assertEquals("svc", j.name);
		assertEquals(8080, j.port);
	}

	@Test
	void preservesExplicitYamlAndJsonExtensions(@TempDir Path dir) {
		AppConfig cfg = new AppConfig();
		cfg.name = "svc";
		cfg.port = 8080;

		Path yaml = dir.resolve("app.yml");
		Path json = dir.resolve("app.json");

		ConfigWriter yamlWriter = new DefaultConfigWriter().withFormat(Format.YAML);
		ConfigWriter jsonWriter = new DefaultConfigWriter().withFormat(Format.JSON);
		yamlWriter.encode(yaml, cfg);
		jsonWriter.encode(json, cfg);

		ConfigReader yamlReader = new DefaultConfigReader().withFormat(Format.YAML);
		ConfigReader jsonReader = new DefaultConfigReader().withFormat(Format.JSON);
		assertEquals(8080, yamlReader.load(yaml, AppConfig.class).port);
		assertEquals(8080, jsonReader.load(json, AppConfig.class).port);
	}

	@Test
	void roundtripsThroughByteArray() {
		AppConfig cfg = new AppConfig();
		cfg.name = "service";
		cfg.port = 8080;

		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		byte[] bytes = writer.encode(cfg);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		AppConfig restored = reader.load(bytes, AppConfig.class);

		assertEquals("service", restored.name);
		assertEquals(8080, restored.port);
	}

	@Test
	void readsEmptyByteArrayAsDefaultInstance() {
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		AppConfig restored = reader.load(new byte[0], AppConfig.class);

		assertNotNull(restored);
		assertNull(restored.name);
		assertEquals(0, restored.port);
	}
}
