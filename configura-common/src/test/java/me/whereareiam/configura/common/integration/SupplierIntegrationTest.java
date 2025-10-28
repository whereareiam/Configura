package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.template.Supplier;
import me.whereareiam.configura.TemplateProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SupplierIntegrationTest {
	static class Cors {
		public boolean enabled;
	}

	public static class CorsSupplier implements TemplateProvider<Cors> {
		@Override
		public Cors supply(Class<Cors> targetType) {
			Cors c = new Cors();
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
		Configuration config = Config.load(dir.resolve("c.yaml").toString(), Configuration.class);

		assertNotNull(config.cors);
		assertTrue(config.cors.enabled);
	}
}


