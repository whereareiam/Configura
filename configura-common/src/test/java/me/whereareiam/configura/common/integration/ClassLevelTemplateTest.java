package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.template.DefaultTemplateRegistry;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ClassLevelTemplateTest {

	public static class ServerConfigProvider implements TemplateProvider<ServerConfig> {
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

	@Template(supplier = @Template.Supplier(ServerConfigProvider.class))
	public static class ServerConfig {
		public String host;
		public int port;
		public boolean ssl;
	}

	public static class DatabaseConfigProvider implements TemplateProvider<DatabaseConfig> {
		@Override
		public DatabaseConfig supply(DatabaseConfig config) {
			config.url = "jdbc:postgresql://localhost:5432/mydb";
			config.username = "admin";
			config.password = "changeme";
			config.maxConnections = 10;
			return config;
		}
	}

	@Template(supplier = @Template.Supplier(DatabaseConfigProvider.class))
	public static class DatabaseConfig {
		public String url;
		public String username;
		public String password;
		public int maxConnections;
	}

	// No template annotation - should have no defaults
	public static class NoTemplateConfig {
		public String value;
		public int number;
	}

	@Test
	void classLevelTemplateAppliesDefaults(@TempDir Path tempDir) {
		ServerConfigProvider.wasCalled = false;
		Path configFile = tempDir.resolve("server.yml");

		TemplateRegistry registry = new DefaultTemplateRegistry();

		ConfigWriter writer = new DefaultConfigWriter(registry).withFormat(Format.YAML);
		ConfigReader reader = new DefaultConfigReader(registry).withFormat(Format.YAML);

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
	void classLevelTemplateOnlyFillsMissingValues(@TempDir Path tempDir) {
		TemplateRegistry registry = new DefaultTemplateRegistry();

		ServerConfig config = new ServerConfig();
		config.host = "example.com"; // User-provided value
		config.port = 9000; // User-provided value
		// ssl not set, should get default

		Path configFile = tempDir.resolve("server.yml");
		ConfigWriter writer = new DefaultConfigWriter(registry).withFormat(Format.YAML);
		writer.encode(configFile, config);

		ConfigReader reader = new DefaultConfigReader(registry).withFormat(Format.YAML);
		ServerConfig loaded = reader.load(configFile, ServerConfig.class);

		// User values preserved
		assertEquals("example.com", loaded.host);
		assertEquals(9000, loaded.port);
		// Default applied for missing
		assertFalse(loaded.ssl);
	}

	@Test
	void classLevelTemplateWorksWithDatabaseConfig(@TempDir Path tempDir) {
		TemplateRegistry registry = new DefaultTemplateRegistry();

		Path configFile = tempDir.resolve("database.yml");

		ConfigWriter writer = new DefaultConfigWriter(registry).withFormat(Format.YAML);
		ConfigReader reader = new DefaultConfigReader(registry).withFormat(Format.YAML);

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
	void classWithoutTemplateHasNoDefaults(@TempDir Path tempDir) {
		TemplateRegistry registry = new DefaultTemplateRegistry();

		NoTemplateConfig config = new NoTemplateConfig();

		Path configFile = tempDir.resolve("no-template.yml");
		ConfigWriter writer = new DefaultConfigWriter(registry).withFormat(Format.YAML);
		writer.encode(configFile, config);

		ConfigReader reader = new DefaultConfigReader(registry).withFormat(Format.YAML);
		NoTemplateConfig loaded = reader.load(configFile, NoTemplateConfig.class);

		// No defaults applied
		assertNull(loaded.value);
		assertEquals(0, loaded.number);
	}

	@Test
	void classLevelTemplateWorksWithUpdatePattern(@TempDir Path tempDir) {
		TemplateRegistry registry = new DefaultTemplateRegistry();

		Path configFile = tempDir.resolve("server-update.yml");

		ConfigWriter writer = new DefaultConfigWriter(registry).withFormat(Format.YAML);
		ConfigReader reader = new DefaultConfigReader(registry).withFormat(Format.YAML);

		// Simulate Config.update() pattern
		ServerConfig defaultInstance = reader.load(new byte[0], ServerConfig.class);
		ServerConfig merged = writer.merge(configFile, defaultInstance);
		writer.write(configFile, merged);

		ServerConfig loaded = reader.load(configFile, ServerConfig.class);

		assertEquals("localhost", loaded.host);
		assertEquals(8080, loaded.port);
		assertFalse(loaded.ssl);
	}

	public static class NestedConfigProvider implements TemplateProvider<ConfigWithNested> {
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

	@Template(supplier = @Template.Supplier(NestedConfigProvider.class))
	public static class ConfigWithNested {
		public String appName;
		public ServerConfig server;
	}

	@Test
	void classLevelTemplateWorksWithNestedObjects(@TempDir Path tempDir) {
		TemplateRegistry registry = new DefaultTemplateRegistry();

		Path configFile = tempDir.resolve("nested.yml");

		ConfigWriter writer = new DefaultConfigWriter(registry).withFormat(Format.YAML);
		ConfigReader reader = new DefaultConfigReader(registry).withFormat(Format.YAML);

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
}