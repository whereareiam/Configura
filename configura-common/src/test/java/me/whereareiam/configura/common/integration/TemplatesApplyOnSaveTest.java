package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TemplatesApplyOnSaveTest {
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
	void templatesFillMissingOnSave(@TempDir Path dir) {
		Path file = dir.resolve("t.yml");
		new DefaultConfigWriter().encode(file, new Cfg());

		DefaultConfigReader reader = new DefaultConfigReader();
		Cfg cfg = reader.load(file.toString(), Cfg.class);

		assertEquals("svc", cfg.name);
		assertNotNull(cfg.policy);
		assertEquals(3, cfg.policy.retries);
	}
}
