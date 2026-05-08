package me.whereareiam.configura;

import me.whereareiam.configura.annotation.Template;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigTemplateSaveRoundtripTest {
	static class AppConfig {
		public ServiceConfig service;
		public DatabaseConfig database;

		public static class ServiceConfig {
			@Template(text = "svc")
			public String name;

			@Template(properties = {
					@Template.Property(name = "retries", number = "3")
			})
			public RetryPolicy retry;

			public static class RetryPolicy {
				public int retries;
			}
		}

		public static class DatabaseConfig {
			public String url;

			@Template(object = @Template.Object(properties = {
					@Template.Property(name = "size", number = "10")
			}))
			public PoolConfig pool;

			public Boolean ssl;

			public static class PoolConfig {
				@Template(number = "10")
				public int size;
			}
		}
	}

	@Test
	void templatesApplyOnSaveAndUserOverridesPersist(@TempDir Path dir) {
		Path file = dir.resolve("app.yml");

		AppConfig initial = new AppConfig();
		initial.service = null;
		initial.database = null;

		Config.save(file, initial);

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

		afterFirstLoad.database.url = "jdbc:postgresql://db/prod";
		afterFirstLoad.service.retry.retries = 5;
		afterFirstLoad.database.ssl = true;
		Config.save(file, afterFirstLoad);

		AppConfig afterSecondLoad = Config.load(file, AppConfig.class);
		assertEquals("jdbc:postgresql://db/prod", afterSecondLoad.database.url);
		assertEquals(5, afterSecondLoad.service.retry.retries);
		assertEquals("svc", afterSecondLoad.service.name);
		assertEquals(10, afterSecondLoad.database.pool.size);
		assertEquals(true, afterSecondLoad.database.ssl);

		Config.save(file, afterSecondLoad);
		AppConfig afterThirdLoad = Config.load(file, AppConfig.class);
		assertEquals(true, afterThirdLoad.database.ssl);
	}
}
