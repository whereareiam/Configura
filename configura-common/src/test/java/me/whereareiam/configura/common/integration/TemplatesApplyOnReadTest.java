package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TemplatesApplyOnReadTest {
	static class Retry {
		public int retries;
	}

	static class Cfg {
		@Field
		@Template(text = "svc")
		public String name;
		@Field
		@Template(properties = {
				@Template.Property(name = "retries", number = "3")
		})
		public Retry policy;
	}

	@Test
	void templatesFillMissingOnLoad(@TempDir Path dir) {
		String base = dir.resolve("t").toString();

		DefaultConfigReader reader = new DefaultConfigReader();
		Cfg cfg = reader.load(base + ".yml", Cfg.class);

		assertEquals("svc", cfg.name);
		assertNotNull(cfg.policy);
		assertEquals(3, cfg.policy.retries);
	}
}


