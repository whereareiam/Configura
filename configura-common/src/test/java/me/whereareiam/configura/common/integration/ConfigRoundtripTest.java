package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigRoundtripTest {
	static class AppConfig {
		public String name;
		public int port;
	}

	@Test
	void roundtripYamlAndJsonViaConfig(@TempDir Path dir) {
		AppConfig cfg = new AppConfig();
		cfg.name = "svc";
		cfg.port = 8080;

		String yaml = dir.resolve("app.yml").toString();
		String json = dir.resolve("app.json").toString();

		ConfigWriter yamlWriter = new DefaultConfigWriter().withFormat(Format.YAML);
		ConfigWriter jsonWriter = new DefaultConfigWriter().withFormat(Format.JSON);
		yamlWriter.save(yaml, cfg);
		jsonWriter.save(json, cfg);

		ConfigReader yamlReader = new DefaultConfigReader().withFormat(Format.YAML);
		ConfigReader jsonReader = new DefaultConfigReader().withFormat(Format.JSON);
		AppConfig y = yamlReader.load(yaml, AppConfig.class);
		AppConfig j = jsonReader.load(json, AppConfig.class);

		assertEquals("svc", y.name);
		assertEquals(8080, y.port);
		assertEquals("svc", j.name);
		assertEquals(8080, j.port);
	}
}


