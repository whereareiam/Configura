package me.whereareiam.configura.merge.defaults;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.annotation.MergeMap;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.type.merge.tree.map.MapPresence;
import me.whereareiam.configura.type.merge.tree.map.MapUnknownEntries;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

	public static class ListenerRegistration {
		public boolean register;
		public String priority;
	}

	public static class ListenerSettingsProvider implements MergeDefaultsProvider<ListenerSettings> {
		@Override
		public ListenerSettings supply(ListenerSettings settings) {
			settings.events = new LinkedHashMap<>();
			settings.events.put("velocity.PreLoginEvent", listener("NORMAL"));
			settings.events.put("velocity.LoginEvent", listener("HIGH"));
			return settings;
		}

		private ListenerRegistration listener(String priority) {
			ListenerRegistration listener = new ListenerRegistration();
			listener.register = true;
			listener.priority = priority;
			return listener;
		}
	}

	@Defaults(provider = @Defaults.Provider(ListenerSettingsProvider.class))
	public static class ListenerSettings {
		@MergeMap(
				presence = MapPresence.DEFAULT_DOMAIN_ONLY,
				unknownEntries = MapUnknownEntries.REJECT
		)
		public Map<String, ListenerRegistration> events;
	}

	public static class RuntimePolicy {
		public Duration ttl;
		public transient Runnable callback;
	}

	public static class RuntimePolicyDefaults implements MergeDefaultsProvider<RuntimeConfig> {
		@Override
		public RuntimeConfig supply(RuntimeConfig config) {
			config.policy = new RuntimePolicy();
			config.policy.ttl = Duration.ofMinutes(5);
			return config;
		}
	}

	@Defaults(provider = @Defaults.Provider(RuntimePolicyDefaults.class))
	public static class RuntimeConfig {
		public RuntimePolicy policy;
	}

	@Test
	void classLevelDefaultsAppliesDefaults(@TempDir Path tempDir) {
		ServerConfigProvider.wasCalled = false;
		Path configFile = tempDir.resolve("server.yml");

		Configura facade = Config.builder().format(Format.YAML).build();
		ConfigReader reader = new DefaultConfigReader(Format.YAML);

		// Simulate Config.update() pattern: load empty, merge, write, read back
		ServerConfig defaultInstance = reader.read(new byte[0], ServerConfig.class);
		ServerConfig merged = facade.merge(configFile, defaultInstance);
		facade.write(configFile, merged);

		assertTrue(ServerConfigProvider.wasCalled, "Provider should have been called during merge");

		ServerConfig loaded = reader.read(configFile, ServerConfig.class);

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
		Configura facade = Config.builder().format(Format.YAML).build();
		facade.save(configFile, config);

		ConfigReader reader = new DefaultConfigReader(Format.YAML);
		ServerConfig loaded = reader.read(configFile, ServerConfig.class);

		// User values preserved
		assertEquals("example.com", loaded.host);
		assertEquals(9000, loaded.port);
		// Default applied for missing
		assertFalse(loaded.ssl);
	}

	@Test
	void classLevelDefaultsWorksWithDatabaseConfig(@TempDir Path tempDir) {
		Path configFile = tempDir.resolve("database.yml");

		Configura facade = Config.builder().format(Format.YAML).build();
		ConfigReader reader = new DefaultConfigReader(Format.YAML);

		DatabaseConfig defaultInstance = reader.read(new byte[0], DatabaseConfig.class);
		DatabaseConfig merged = facade.merge(configFile, defaultInstance);
		facade.write(configFile, merged);

		DatabaseConfig loaded = reader.read(configFile, DatabaseConfig.class);

		assertEquals("jdbc:postgresql://localhost:5432/mydb", loaded.url);
		assertEquals("admin", loaded.username);
		assertEquals("changeme", loaded.password);
		assertEquals(10, loaded.maxConnections);
	}

	@Test
	void classWithoutDefaultsHasNoDefaults(@TempDir Path tempDir) {
		NoDefaultsConfig config = new NoDefaultsConfig();

		Path configFile = tempDir.resolve("no-defaults.yml");
		Configura facade = Config.builder().format(Format.YAML).build();
		facade.save(configFile, config);

		ConfigReader reader = new DefaultConfigReader(Format.YAML);
		NoDefaultsConfig loaded = reader.read(configFile, NoDefaultsConfig.class);

		// No defaults applied
		assertNull(loaded.value);
		assertEquals(0, loaded.number);
	}

	@Test
	void classLevelDefaultsWorksWithUpdatePattern(@TempDir Path tempDir) {
		Path configFile = tempDir.resolve("server-update.yml");

		Configura facade = Config.builder().format(Format.YAML).build();
		ServerConfig loaded = facade.update(configFile, ServerConfig.class);

		assertEquals("localhost", loaded.host);
		assertEquals(8080, loaded.port);
		assertFalse(loaded.ssl);
	}

	@Test
	void classLevelDefaultsSkipTransientFieldsAndNonInstantiableLeafTypes(@TempDir Path tempDir) {
		Path configFile = tempDir.resolve("runtime.yml");

		Configura facade = Config.builder().format(Format.YAML).build();
		RuntimeConfig loaded = facade.update(configFile, RuntimeConfig.class);

		assertNotNull(loaded.policy);
		assertEquals(Duration.ofMinutes(5), loaded.policy.ttl);
		assertNull(loaded.policy.callback);
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

		Configura facade = Config.builder().format(Format.YAML).build();
		ConfigReader reader = new DefaultConfigReader(Format.YAML);

		ConfigWithNested defaultInstance = reader.read(new byte[0], ConfigWithNested.class);
		ConfigWithNested merged = facade.merge(configFile, defaultInstance);
		facade.write(configFile, merged);

		ConfigWithNested loaded = reader.read(configFile, ConfigWithNested.class);

		assertEquals("MyApp", loaded.appName);
		assertNotNull(loaded.server);
		assertEquals("0.0.0.0", loaded.server.host);
		assertEquals(3000, loaded.server.port);
		assertTrue(loaded.server.ssl);
	}

	@Test
	void providerDefaultsPopulateMissingFieldOnSave(@TempDir Path dir) {
		Path file = dir.resolve("provider.yml");
		Config.configured().save(file, new ProviderConfig());

		ProviderConfig config = new DefaultConfigReader().read(file, ProviderConfig.class);

		assertNotNull(config.cors);
		assertTrue(config.cors.enabled);
	}

	@Test
	void inlineDefaultsFillMissingValuesOnSave(@TempDir Path dir) {
		Path file = dir.resolve("save.yml");
		Config.configured().save(file, new SaveConfig());

		SaveConfig config = new DefaultConfigReader().read(file, SaveConfig.class);

		assertEquals("svc", config.name);
		assertNotNull(config.policy);
		assertEquals(3, config.policy.retries);
	}

	@Test
	void defaultDomainOnlyMapRejectsStaleKeys(@TempDir Path dir) {
		Path file = dir.resolve("listeners.yml");
		ConfigWriter writer = Config.writer(Format.YAML);
		ConfigReader reader = new DefaultConfigReader(Format.YAML);

		ListenerSettings existing = new ListenerSettings();
		existing.events = new LinkedHashMap<>();
		ListenerRegistration preLogin = new ListenerRegistration();
		preLogin.register = false;
		existing.events.put("velocity.PreLoginEvent", preLogin);
		ListenerRegistration stale = new ListenerRegistration();
		stale.priority = "LOW";
		existing.events.put("bungeecord.PostLoginEvent", stale);
		writer.write(file, existing);

		ListenerSettings defaultInstance = reader.read(new byte[0], ListenerSettings.class);
		Configura facade = Config.builder().format(Format.YAML).build();
		assertThrows(ConfigException.class, () -> facade.merge(file, defaultInstance));
	}
}
