package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.template.Supplier;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SupplierIntegrationTest {
    public static class Cors {
		public boolean enabled;
	}

	public static class CorsSupplier implements TemplateProvider<Cors> {
		@Override
		public Cors supply(Cors c) {
			c.enabled = true;
			return c;
		}
	}

	static class Configuration {
		@Field
		@Supplier(CorsSupplier.class)
		public Cors cors;
	}

	@Test
	void supplierPopulatesFieldWhenMissing(@TempDir Path dir) {
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		Configuration config = reader.load(dir.resolve("c.yaml").toString(), Configuration.class);

		assertNotNull(config.cors);
		assertTrue(config.cors.enabled);
	}
}


