package me.whereareiam.configura.common.reader;

import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DefaultConfigReaderIntegrationTest {
	static class AppConfig {
		public String name;
		public int port;
	}

	@Test
	void readsYamlAndJsonFiles(@TempDir Path dir) {
		Path yaml = dir.resolve("app.yml");
		Path json = dir.resolve("app.json");
		try {
			Files.writeString(yaml, "name: svc\nport: 8080\n");
			Files.writeString(json, "{\n  \"name\": \"svc\",\n  \"port\": 8080\n}\n");
		} catch (Exception e) {
			throw new AssertionError(e);
		}

		DefaultConfigReader yamlReader = new DefaultConfigReader(Format.YAML);
		DefaultConfigReader jsonReader = new DefaultConfigReader(Format.JSON);
		AppConfig yamlConfig = yamlReader.read(yaml, AppConfig.class);
		AppConfig jsonConfig = jsonReader.read(json, AppConfig.class);

		assertEquals("svc", yamlConfig.name);
		assertEquals(8080, yamlConfig.port);
		assertEquals("svc", jsonConfig.name);
		assertEquals(8080, jsonConfig.port);
	}

	@Test
	void readsEmptyByteArrayAsDefaultInstance() {
		DefaultConfigReader reader = new DefaultConfigReader();

		AppConfig restored = reader.read(new byte[0], AppConfig.class);

		assertNotNull(restored);
		assertNull(restored.name);
		assertEquals(0, restored.port);
	}
}
