package me.whereareiam.configura.common.merge.defaults;

import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MergeDefaultsIntegrationTest {

	public static class ServerConfigProvider implements MergeDefaultsProvider<ServerConfig> {
		public static boolean wasCalled = false;

		@Override
		public ServerConfig supply(ServerConfig config) {
			wasCalled = true;
			config.host = "localhost";
			config.port = 8080;
			config.ssl = false;
			return config;
		}
	}

	@Defaults(provider = @Defaults.Provider(ServerConfigProvider.class))
	public static class ServerConfig {
		public String host;
		public int port;
		public boolean ssl;
	}

	public static class DatabaseConfigProvider implements MergeDefaultsProvider<DatabaseConfig> {
		@Override
		public DatabaseConfig supply(DatabaseConfig config) {
			config.url = "jdbc:postgresql://localhost:5432/mydb";
			config.username = "admin";
			config.password = "changeme";
			config.maxConnections = 10;
			return config;
		}
	}

	@Defaults(provider = @Defaults.Provider(DatabaseConfigProvider.class))
	public static class DatabaseConfig {
		public String url;
		public String username;
		public String password;
		public int maxConnections;
	}

	// No defaults annotation - should have no defaults
	public static class NoDefaultsConfig {
		public String value;
		public int number;
	}

	static class Retry {
		public int retries;
	}

	static class SaveConfig {
		@Defaults(text = "svc")
		public String name;

		@Defaults(properties = {
				@Defaults.Property(name = "retries", number = "3")
		})
		public Retry policy;
	}

	public static class Cors {
		public boolean enabled;
	}

	public static class CorsProvider implements MergeDefaultsProvider<Cors> {
		@Override
		public Cors supply(Cors cors) {
			cors.enabled = true;
			return cors;
		}
	}

	static class ProviderConfig {
		@Defaults(provider = @Defaults.Provider(CorsProvider.class))
		public Cors cors;
	}

	@Test
	void classLevelDefaultsAppliesDefaults(@TempDir Path tempDir) {
		ServerConfigProvider.wasCalled = false;
		Path configFile = tempDir.resolve("server.yml");

		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);

		// Simulate Config.update() pattern: load empty, merge, write, read back
		ServerConfig defaultInstance = reader.load(new byte[0], ServerConfig.class);
		ServerConfig merged = writer.merge(configFile, defaultInstance);
		writer.write(configFile, merged);

		assertTrue(ServerConfigProvider.wasCalled, "Provider should have been called during merge");

		ServerConfig loaded = reader.load(configFile, ServerConfig.class);

		assertEquals("localhost", loaded.host);
		assertEquals(8080, loaded.port);
		assertFalse(loaded.ssl);
	}

	@Test
	void classLevelDefaultsOnlyFillsMissingValues(@TempDir Path tempDir) {
		ServerConfig config = new ServerConfig();
		config.host = "example.com"; // User-provided value
		config.port = 9000; // User-provided value
		// ssl not set, should get default

		Path configFile = tempDir.resolve("server.yml");
		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		writer.encode(configFile, config);

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		ServerConfig loaded = reader.load(configFile, ServerConfig.class);

		// User values preserved
		assertEquals("example.com", loaded.host);
		assertEquals(9000, loaded.port);
		// Default applied for missing
		assertFalse(loaded.ssl);
	}

	@Test
	void classLevelDefaultsWorksWithDatabaseConfig(@TempDir Path tempDir) {
		Path configFile = tempDir.resolve("database.yml");

		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);

		DatabaseConfig defaultInstance = reader.load(new byte[0], DatabaseConfig.class);
		DatabaseConfig merged = writer.merge(configFile, defaultInstance);
		writer.write(configFile, merged);

		DatabaseConfig loaded = reader.load(configFile, DatabaseConfig.class);

		assertEquals("jdbc:postgresql://localhost:5432/mydb", loaded.url);
		assertEquals("admin", loaded.username);
		assertEquals("changeme", loaded.password);
		assertEquals(10, loaded.maxConnections);
	}

	@Test
	void classWithoutDefaultsHasNoDefaults(@TempDir Path tempDir) {
		NoDefaultsConfig config = new NoDefaultsConfig();

		Path configFile = tempDir.resolve("no-defaults.yml");
		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		writer.encode(configFile, config);

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		NoDefaultsConfig loaded = reader.load(configFile, NoDefaultsConfig.class);

		// No defaults applied
		assertNull(loaded.value);
		assertEquals(0, loaded.number);
	}

	@Test
	void classLevelDefaultsWorksWithUpdatePattern(@TempDir Path tempDir) {
		Path configFile = tempDir.resolve("server-update.yml");

		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);

		// Simulate Config.update() pattern
		ServerConfig defaultInstance = reader.load(new byte[0], ServerConfig.class);
		ServerConfig merged = writer.merge(configFile, defaultInstance);
		writer.write(configFile, merged);

		ServerConfig loaded = reader.load(configFile, ServerConfig.class);

		assertEquals("localhost", loaded.host);
		assertEquals(8080, loaded.port);
		assertFalse(loaded.ssl);
	}

	public static class NestedConfigProvider implements MergeDefaultsProvider<ConfigWithNested> {
		@Override
		public ConfigWithNested supply(ConfigWithNested config) {
			config.appName = "MyApp";
			if (config.server == null) {
				config.server = new ServerConfig();
			}
			config.server.host = "0.0.0.0";
			config.server.port = 3000;
			config.server.ssl = true;
			return config;
		}
	}

	@Defaults(provider = @Defaults.Provider(NestedConfigProvider.class))
	public static class ConfigWithNested {
		public String appName;
		public ServerConfig server;
	}

	@Test
	void classLevelDefaultsWorksWithNestedObjects(@TempDir Path tempDir) {
		Path configFile = tempDir.resolve("nested.yml");

		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);

		ConfigWithNested defaultInstance = reader.load(new byte[0], ConfigWithNested.class);
		ConfigWithNested merged = writer.merge(configFile, defaultInstance);
		writer.write(configFile, merged);

		ConfigWithNested loaded = reader.load(configFile, ConfigWithNested.class);

		assertEquals("MyApp", loaded.appName);
		assertNotNull(loaded.server);
		assertEquals("0.0.0.0", loaded.server.host);
		assertEquals(3000, loaded.server.port);
		assertTrue(loaded.server.ssl);
	}

	@Test
	void providerDefaultsPopulateMissingFieldOnSave(@TempDir Path dir) {
		Path file = dir.resolve("provider.yml");
		new DefaultConfigWriter().encode(file, new ProviderConfig());

		ProviderConfig config = new DefaultConfigReader().load(file.toString(), ProviderConfig.class);

		assertNotNull(config.cors);
		assertTrue(config.cors.enabled);
	}

	@Test
	void inlineDefaultsFillMissingValuesOnSave(@TempDir Path dir) {
		Path file = dir.resolve("save.yml");
		new DefaultConfigWriter().encode(file, new SaveConfig());

		SaveConfig config = new DefaultConfigReader().load(file.toString(), SaveConfig.class);

		assertEquals("svc", config.name);
		assertNotNull(config.policy);
		assertEquals(3, config.policy.retries);
	}
}
