package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateSeedingTest {
	static class Retry {
		public int retries;
		public Backoff backoff;
	}

	static class Backoff {
		public long initialMs;
		public long maxMs;
	}

	static class Cfg {
		@Template(text = "svc")
		public String name;
		@Template(properties = {
				@Template.Property(name = "retries", number = "3")
		})
			public Retry policy;
	}

	static class SimpleHolder {
		@Template(text = "x")
		public String value;
	}

	static class SeedHolder {
		@Template(stringItems = {"a", "b"})
		public List<String> names;

		@Template(properties = {
				@Template.Property(name = "retries", number = "3")
		})
		public Retry policy;
	}

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

	static class SupplierConfig {
		@Template(supplier = @Template.Supplier(CorsSupplier.class))
		public Cors cors;
	}

	@Test
	void seederAppliesTextTemplateWhenValueIsMissing() {
		ObjectMapper mapper = new ObjectMapper();
		SimpleHolder holder = new SimpleHolder();
		holder.value = null;

		new TemplateSeeder(mapper, null).seed(holder);

		assertEquals("x", holder.value);
	}

	@Test
	void seederBindsListLiteral() {
		ObjectMapper mapper = new ObjectMapper();
		SeedHolder holder = new SeedHolder();
		holder.names = null;

		new TemplateSeeder(mapper, null).seed(holder);

		assertNotNull(holder.names);
		assertEquals(List.of("a", "b"), holder.names);
	}

	@Test
	void seederBindsObjectLiteralPartially() {
		ObjectMapper mapper = new ObjectMapper();
		SeedHolder holder = new SeedHolder();
		holder.policy = null;

		new TemplateSeeder(mapper, null).seed(holder);

		assertNotNull(holder.policy);
		assertEquals(3, holder.policy.retries);
		assertNull(holder.policy.backoff);
	}

	@Test
	void supplierTemplatePopulatesMissingFieldOnSave(@TempDir Path dir) {
		Path file = dir.resolve("c.yml");
		new DefaultConfigWriter().encode(file, new SupplierConfig());

		DefaultConfigReader reader = new DefaultConfigReader();
		SupplierConfig config = reader.load(file.toString(), SupplierConfig.class);

		assertNotNull(config.cors);
		assertTrue(config.cors.enabled);
	}

	@Test
	void inlineTemplatesFillMissingValuesOnSave(@TempDir Path dir) {
		Path file = dir.resolve("t.yml");
		new DefaultConfigWriter().encode(file, new Cfg());

		DefaultConfigReader reader = new DefaultConfigReader();
		Cfg cfg = reader.load(file.toString(), Cfg.class);

		assertEquals("svc", cfg.name);
		assertNotNull(cfg.policy);
		assertEquals(3, cfg.policy.retries);
	}
}
