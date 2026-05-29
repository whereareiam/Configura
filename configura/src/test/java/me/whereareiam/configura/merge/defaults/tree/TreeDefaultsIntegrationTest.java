package me.whereareiam.configura.merge.defaults.tree;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.Defaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TreeDefaultsIntegrationTest {
	static class AppConfig {
		public ServiceConfig service;
		public DatabaseConfig database;

		public static class ServiceConfig {
			@Defaults(text = "svc")
			public String name;

			@Defaults(properties = {
					@Defaults.Property(name = "retries", number = "3")
			})
			public RetryPolicy retry;

			public static class RetryPolicy {
				public int retries;
			}
		}

		public static class DatabaseConfig {
			public String url;

			@Defaults(object = @Defaults.Object(properties = {
					@Defaults.Property(name = "size", number = "10")
			}))
			public PoolConfig pool;

			public Boolean ssl;

			public static class PoolConfig {
				@Defaults(number = "10")
				public int size;
			}
		}
	}

	@Test
	void defaultsApplyOnSaveAndUserOverridesPersist(@TempDir Path dir) {
		Path file = dir.resolve("app.yml");

		AppConfig initial = new AppConfig();
		initial.service = null;
		initial.database = null;

		Config.configured().save(file, initial);

		AppConfig afterFirstLoad = Config.configured().read(file, AppConfig.class);
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
		Config.configured().save(file, afterFirstLoad);

		AppConfig afterSecondLoad = Config.configured().read(file, AppConfig.class);
		assertEquals("jdbc:postgresql://db/prod", afterSecondLoad.database.url);
		assertEquals(5, afterSecondLoad.service.retry.retries);
		assertEquals("svc", afterSecondLoad.service.name);
		assertEquals(10, afterSecondLoad.database.pool.size);
		assertEquals(true, afterSecondLoad.database.ssl);

		Config.configured().save(file, afterSecondLoad);
		AppConfig afterThirdLoad = Config.configured().read(file, AppConfig.class);
		assertEquals(true, afterThirdLoad.database.ssl);
	}

	@Test
	void saveDropsFieldsMissingFromCurrentModel(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("app.yml");
		Files.writeString(file, """
				service:
				  name: kept
				legacy: true
				""");

		AppConfig config = Config.configured().read(file, AppConfig.class);
		Config.configured().save(file, config);

		String persisted = Files.readString(file);
		assertEquals("kept", Config.configured().read(file, AppConfig.class).service.name);
		assertFalse(persisted.contains("legacy:"));
	}
}
