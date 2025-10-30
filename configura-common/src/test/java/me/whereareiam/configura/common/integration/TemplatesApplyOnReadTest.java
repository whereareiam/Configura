package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.template.Template;
import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.annotation.template.type.Property;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
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
		@Template(literal = @Literal(text = "svc"))
		public String name;
		@Field
		@Template(properties = {@Property(name = "retries", value = @Literal(number = "3"))})
		public Retry policy;
	}

	@Test
	void templatesFillMissingOnLoad(@TempDir Path dir) {
		String base = dir.resolve("t").toString();

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		Cfg cfg = reader.load(base + ".yaml", Cfg.class);

		assertEquals("svc", cfg.name);
		assertNotNull(cfg.policy);
		assertEquals(3, cfg.policy.retries);
	}
}


