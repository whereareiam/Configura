package me.whereareiam.configura.integration;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.annotation.Policy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTemplatesOnSaveTest {
	static class AppConfig {
		@Field
		public ServiceConfig service;

		@Field
		public DatabaseConfig database;

		public static class ServiceConfig {
			@Field
			@Template(text = "svc")
			public String name;

			@Field
			@Template(properties = {
					@Template.Property(name = "retries", number = "3")
			})
			public RetryPolicy retry;

			public static class RetryPolicy {
				public int retries;
			}
		}

		public static class DatabaseConfig {
			@Field
			public String url;

			@Field
			@Template(object = @Template.Object(properties = {
					@Template.Property(name = "size", number = "10")
			}))
			public PoolConfig pool;

			@Field
			@Policy(mergeOnUpdate = false)
			public Boolean ssl;

			public static class PoolConfig {
				@Field
				@Template(number = "10")
				public int size;
			}
		}
	}

	@Test
	void templatesApplyOnSave_andUserOverridesPersist(@TempDir Path dir) {
		Path file = dir.resolve("app.yml");

		// Start with a model whose nested objects are not directly instantiated
		AppConfig initial = new AppConfig();
		initial.service = null;
		initial.database = null;

		// First save: templates should instantiate nested objects and set defaults
		Config.save(file, initial);

		// First load: defaults should be present
		AppConfig afterFirstLoad = Config.load(file, AppConfig.class);
		assertNotNull(afterFirstLoad.service);
		assertEquals("svc", afterFirstLoad.service.name);
		assertNotNull(afterFirstLoad.service.retry);
		assertEquals(3, afterFirstLoad.service.retry.retries);
		assertNotNull(afterFirstLoad.database);
		assertNull(afterFirstLoad.database.url);
		assertNotNull(afterFirstLoad.database.pool);
		assertEquals(10, afterFirstLoad.database.pool.size);
		assertNull(afterFirstLoad.database.ssl);

		// Modify some values and save again
		afterFirstLoad.database.url = "jdbc:postgresql://db/prod";
		afterFirstLoad.service.retry.retries = 5;
		afterFirstLoad.database.ssl = true;
		Config.save(file, afterFirstLoad);

		// Second load: user overrides should persist, defaults remain for other fields
		AppConfig afterSecondLoad = Config.load(file, AppConfig.class);
		assertEquals("jdbc:postgresql://db/prod", afterSecondLoad.database.url);
		assertEquals(5, afterSecondLoad.service.retry.retries);
		assertEquals("svc", afterSecondLoad.service.name);
		assertEquals(10, afterSecondLoad.database.pool.size);
		assertEquals(true, afterSecondLoad.database.ssl);

		// Save again without changing ssl; value should remain true
		Config.save(file, afterSecondLoad);
		AppConfig afterThirdLoad = Config.load(file, AppConfig.class);
		assertEquals(true, afterThirdLoad.database.ssl);
	}
}
