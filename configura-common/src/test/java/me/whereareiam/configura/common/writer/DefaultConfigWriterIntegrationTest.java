package me.whereareiam.configura.common.writer;

import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConfigWriterIntegrationTest {
	static class AppConfig {
		public String name;
		public int port;
	}

	static class OldConfig {
		public String host;
		public int port;
	}

	static class NewConfig {
		public String host;
		public int port;
	}

	@Test
	void writesYamlAndJsonFilesReadableByDefaultReader(@TempDir Path dir) {
		AppConfig cfg = new AppConfig();
		cfg.name = "svc";
		cfg.port = 8080;

		Path yaml = dir.resolve("app.yml");
		Path json = dir.resolve("app.json");

		DefaultConfigWriter yamlWriter = (DefaultConfigWriter) new DefaultConfigWriter().withFormat(Format.YAML);
		DefaultConfigWriter jsonWriter = (DefaultConfigWriter) new DefaultConfigWriter().withFormat(Format.JSON);
		yamlWriter.encode(yaml, cfg);
		jsonWriter.encode(json, cfg);

		DefaultConfigReader yamlReader = (DefaultConfigReader) new DefaultConfigReader().withFormat(Format.YAML);
		DefaultConfigReader jsonReader = (DefaultConfigReader) new DefaultConfigReader().withFormat(Format.JSON);
		assertEquals(8080, yamlReader.load(yaml, AppConfig.class).port);
		assertEquals(8080, jsonReader.load(json, AppConfig.class).port);
	}

	@Test
	void preservesExplicitYamlAndJsonExtensions(@TempDir Path dir) {
		AppConfig cfg = new AppConfig();
		cfg.name = "svc";
		cfg.port = 8080;

		Path yaml = dir.resolve("app.yml");
		Path json = dir.resolve("app.json");

		DefaultConfigWriter yamlWriter = (DefaultConfigWriter) new DefaultConfigWriter().withFormat(Format.YAML);
		DefaultConfigWriter jsonWriter = (DefaultConfigWriter) new DefaultConfigWriter().withFormat(Format.JSON);
		yamlWriter.encode(yaml, cfg);
		jsonWriter.encode(json, cfg);

		assertTrue(Files.exists(yaml));
		assertTrue(Files.exists(json));
	}

	@Test
	void encodesByteArrayReadableByDefaultReader() {
		AppConfig cfg = new AppConfig();
		cfg.name = "service";
		cfg.port = 8080;

		DefaultConfigWriter writer = new DefaultConfigWriter();
		byte[] bytes = writer.encode(cfg);

		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
		assertEquals("service", new DefaultConfigReader().load(bytes, AppConfig.class).name);
	}

	@Test
	void encodePrunesUnknownPropertiesDuringRewrite(@TempDir Path dir) throws Exception {
		String base = dir.resolve("app").toString();

		OldConfig oldConfig = new OldConfig();
		oldConfig.host = "0.0.0.0";
		oldConfig.port = 9000;

		DefaultConfigWriter writer = (DefaultConfigWriter) new DefaultConfigWriter().withFormat(Format.YAML);
		writer.encode(base + ".yml", oldConfig);

		Path yaml = Path.of(base + ".yml");
		Files.writeString(yaml, Files.readString(yaml) + "\nunknown: 1\n", StandardCharsets.UTF_8);

		NewConfig newConfig = new NewConfig();
		newConfig.host = "0.0.0.0";
		newConfig.port = 9000;

		writer.encode(base + ".yml", newConfig);
		NewConfig after = new DefaultConfigReader().withFormat(Format.YAML).load(base + ".yml", NewConfig.class);

		assertFalse(Files.readString(yaml).contains("unknown:"));
		assertEquals("0.0.0.0", after.host);
		assertEquals(9000, after.port);
	}
}
