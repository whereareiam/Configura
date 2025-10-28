package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.Config;
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

		String yaml = dir.resolve("app.yaml").toString();
		String json = dir.resolve("app.json").toString();

		Config.save(yaml, cfg);
		Config.save(json, cfg);

		AppConfig y = Config.load(yaml, AppConfig.class);
		AppConfig j = Config.load(json, AppConfig.class);

		assertEquals("svc", y.name);
		assertEquals(8080, y.port);
		assertEquals("svc", j.name);
		assertEquals(8080, j.port);
	}
}


