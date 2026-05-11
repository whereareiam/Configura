package me.whereareiam.configura.common.reader;

import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultConfigReaderIntegrationTest {
	static class AppConfig {
		public String name;
		public int port;
	}

	@Test
	void readsYamlAndJsonFiles(@TempDir Path dir) {
		AppConfig cfg = new AppConfig();
		cfg.name = "svc";
		cfg.port = 8080;

		Path yaml = dir.resolve("app.yml");
		Path json = dir.resolve("app.json");

		ConfigWriter yamlWriter = new DefaultConfigWriter().withFormat(Format.YAML);
		ConfigWriter jsonWriter = new DefaultConfigWriter().withFormat(Format.JSON);
		yamlWriter.encode(yaml, cfg);
		jsonWriter.encode(json, cfg);

		DefaultConfigReader yamlReader = (DefaultConfigReader) new DefaultConfigReader().withFormat(Format.YAML);
		DefaultConfigReader jsonReader = (DefaultConfigReader) new DefaultConfigReader().withFormat(Format.JSON);
		AppConfig yamlConfig = yamlReader.load(yaml, AppConfig.class);
		AppConfig jsonConfig = jsonReader.load(json, AppConfig.class);

		assertEquals("svc", yamlConfig.name);
		assertEquals(8080, yamlConfig.port);
		assertEquals("svc", jsonConfig.name);
		assertEquals(8080, jsonConfig.port);
	}

	@Test
	void readsEmptyByteArrayAsDefaultInstance() {
		DefaultConfigReader reader = new DefaultConfigReader();

		AppConfig restored = reader.load(new byte[0], AppConfig.class);

		assertNotNull(restored);
		assertNull(restored.name);
		assertEquals(0, restored.port);
	}
}
