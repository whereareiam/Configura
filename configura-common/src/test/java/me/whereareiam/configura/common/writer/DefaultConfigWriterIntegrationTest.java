package me.whereareiam.configura.common.writer;

import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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

		DefaultConfigWriter yamlWriter = new DefaultConfigWriter(Format.YAML);
		DefaultConfigWriter jsonWriter = new DefaultConfigWriter(Format.JSON);
		yamlWriter.write(yaml, cfg);
		jsonWriter.write(json, cfg);

		DefaultConfigReader yamlReader = new DefaultConfigReader(Format.YAML);
		DefaultConfigReader jsonReader = new DefaultConfigReader(Format.JSON);
		assertEquals(8080, yamlReader.read(yaml, AppConfig.class).port);
		assertEquals(8080, jsonReader.read(json, AppConfig.class).port);
	}

	@Test
	void preservesExplicitYamlAndJsonExtensions(@TempDir Path dir) {
		AppConfig cfg = new AppConfig();
		cfg.name = "svc";
		cfg.port = 8080;

		Path yaml = dir.resolve("app.yml");
		Path json = dir.resolve("app.json");

		DefaultConfigWriter yamlWriter = new DefaultConfigWriter(Format.YAML);
		DefaultConfigWriter jsonWriter = new DefaultConfigWriter(Format.JSON);
		yamlWriter.write(yaml, cfg);
		jsonWriter.write(json, cfg);

		assertTrue(Files.exists(yaml));
		assertTrue(Files.exists(json));
	}

	@Test
	void encodesByteArrayReadableByDefaultReader() {
		AppConfig cfg = new AppConfig();
		cfg.name = "service";
		cfg.port = 8080;

		DefaultConfigWriter writer = new DefaultConfigWriter();
		byte[] bytes = writer.writeBytes(cfg);

		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
		assertEquals("service", new DefaultConfigReader().read(bytes, AppConfig.class).name);
	}

	@Test
	void writePrunesUnknownPropertiesDuringRewrite(@TempDir Path dir) throws Exception {
		String base = dir.resolve("app").toString();

		OldConfig oldConfig = new OldConfig();
		oldConfig.host = "0.0.0.0";
		oldConfig.port = 9000;

		DefaultConfigWriter writer = new DefaultConfigWriter(Format.YAML);
		writer.write(Path.of(base + ".yml"), oldConfig);

		Path yaml = Path.of(base + ".yml");
		Files.writeString(yaml, Files.readString(yaml) + "\nunknown: 1\n", StandardCharsets.UTF_8);

		NewConfig newConfig = new NewConfig();
		newConfig.host = "0.0.0.0";
		newConfig.port = 9000;

		writer.write(Path.of(base + ".yml"), newConfig);
		NewConfig after = new DefaultConfigReader(Format.YAML).read(Path.of(base + ".yml"), NewConfig.class);

		assertFalse(Files.readString(yaml).contains("unknown:"));
		assertEquals("0.0.0.0", after.host);
		assertEquals(9000, after.port);
	}
}
