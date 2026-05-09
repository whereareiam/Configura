package me.whereareiam.configura;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.SchemaVersion;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.migration.ConfigMigrationStep;
import me.whereareiam.configura.migration.ConfigMigrationContext;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Config Versioning")
class SchemaVersioningTest {
	@Test
	@DisplayName("ConfigDocument migrates in memory without rewriting the file")
	void configDocumentMigratesInMemoryWithoutRewrite(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("settings.yml");
		Files.writeString(file, """
				connection:
				  routing:
				    defaultProxy: proxy-auth
				""");

		Config config = versionedSettingsConfig();
		SettingsConfig settings = config.read(file, SettingsConfig.class);

		assertEquals(1, settings.getVersion());
		assertEquals("proxy-auth", settings.connection.routing.defaults.step.target);

		String rawFile = Files.readString(file);
		assertFalse(rawFile.contains("_version"));
		assertTrue(rawFile.contains("defaultProxy"));

		JsonNode rawTree = config.readNode(file);
		assertTrue(rawTree.path("_version").isMissingNode());

		JsonNode migratedTree = config.readMigratedNode(file, SettingsConfig.class);
		assertEquals(1, migratedTree.path("_version").asInt());
		assertEquals("proxy-auth", migratedTree.path("connection").path("routing").path("defaults").path("step").path("target").asText());
		assertTrue(migratedTree.path("connection").path("routing").path("defaultProxy").isMissingNode());
	}

	@Test
	@DisplayName("ConfigDocument update persists the latest _version")
	void configDocumentUpdatePersistsLatestVersion(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("settings.yml");
		Files.writeString(file, """
				connection:
				  routing:
				    defaultProxy: proxy-auth
				""");

		Config config = versionedSettingsConfig();
		SettingsConfig settings = config.update(file, SettingsConfig.class);

		assertEquals(1, settings.getVersion());
		assertEquals("proxy-auth", settings.connection.routing.defaults.step.target);

		JsonNode persisted = config.readNode(file);
		assertEquals(1, persisted.path("_version").asInt());
		assertEquals("proxy-auth", persisted.path("connection").path("routing").path("defaults").path("step").path("target").asText());
		assertTrue(persisted.path("connection").path("routing").path("defaultProxy").isMissingNode());
	}

	@Test
	@DisplayName("ConfigDocument update backs up the file before persisting a migrated version by default")
	void configDocumentUpdateBacksUpMigratedFileByDefault(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("settings.yml");
		Files.writeString(file, """
				connection:
				  routing:
				    defaultProxy: proxy-auth
				""");

		Config config = versionedSettingsConfig();
		config.update(file, SettingsConfig.class);

		Path backup = file.resolveSibling("settings.yml.bak");
		assertTrue(Files.exists(backup));

		String backupContent = Files.readString(backup);
		assertTrue(backupContent.contains("defaultProxy: proxy-auth"));
		assertFalse(backupContent.contains("_version"));
	}

	@Test
	@DisplayName("ConfigDocument update skips backup when migration backup is disabled")
	void configDocumentUpdateSkipsBackupWhenDisabled(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("settings.yml");
		Files.writeString(file, """
				connection:
				  routing:
				    defaultProxy: proxy-auth
				""");

		Config config = Config.builder()
				.format(Format.YAML)
				.backupOnMigration(false)
				.versioned(SettingsConfig.class, spec -> spec
						.currentVersion(1)
						.migration(new SettingsMigrationV0ToV1()))
				.build();

		config.update(file, SettingsConfig.class);

		assertFalse(Files.exists(file.resolveSibling("settings.yml.bak")));
	}

	@Test
	@DisplayName("Custom @SchemaVersion field is picked up automatically")
	void customConfigVersionFieldIsPickedUpAutomatically(@TempDir Path tempDir) {
		Config config = versionedCustomVersionConfig();
		Path file = tempDir.resolve("custom-settings");

		CustomVersionConfig settings = new CustomVersionConfig();
		settings.label = "ready";

		config.write(file, settings);

		JsonNode persisted = config.readNode(file);
		assertEquals(1, persisted.path("schemaVersion").asInt());
		assertTrue(persisted.path("_version").isMissingNode());
		assertEquals("ready", persisted.path("label").asText());
	}

	@Test
	@DisplayName("Types without a version field assume zero and do not persist one")
	void typesWithoutVersionFieldDoNotPersistVersion(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("plain.yml");
		Files.writeString(file, """
				connection:
				  routing:
				    defaultProxy: proxy-auth
				""");

		Config config = versionedPlainSettingsConfig();
		PlainSettingsConfig settings = config.update(file, PlainSettingsConfig.class);

		assertEquals("proxy-auth", settings.connection.routing.defaults.step.target);

		JsonNode persisted = config.readNode(file);
		assertTrue(persisted.path("_version").isMissingNode());
		assertEquals("proxy-auth", persisted.path("connection").path("routing").path("defaults").path("step").path("target").asText());
	}

	@Test
	@DisplayName("Existing raw _version is used as fallback and preserved")
	void existingRawVersionIsUsedAsFallback(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("fallback.yml");
		Files.writeString(file, """
				_version: 1
				label: legacy-user
				""");

		Config config = versionedFallbackConfig();
		FallbackProfileConfig profile = config.update(file, FallbackProfileConfig.class);

		assertEquals("legacy-user", profile.profile.displayName);

		JsonNode persisted = config.readNode(file);
		assertEquals(2, persisted.path("_version").asInt());
		assertEquals("legacy-user", persisted.path("profile").path("displayName").asText());
		assertTrue(persisted.path("label").isMissingNode());
	}

	@Test
	@DisplayName("Migration steps run in version order regardless of registration order")
	void migrationStepsRunInOrder(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("profile.yml");
		Files.writeString(file, """
				name: legacy-user
				""");

		Config config = Config.builder()
				.format(Format.YAML)
				.versioned(ProfileConfig.class, spec -> spec
						.currentVersion(2)
						.migration(new ProfileMigrationV1ToV2())
						.migration(new ProfileMigrationV0ToV1()))
				.build();

		ProfileConfig profile = config.update(file, ProfileConfig.class);

		assertEquals(2, profile.getVersion());
		assertEquals("legacy-user", profile.profile.displayName);

		JsonNode persisted = config.readNode(file);
		assertEquals(2, persisted.path("_version").asInt());
		assertEquals("legacy-user", persisted.path("profile").path("displayName").asText());
		assertTrue(persisted.path("name").isMissingNode());
		assertTrue(persisted.path("displayName").isMissingNode());
	}

	@Test
	@DisplayName("Exact writes stamp the current version for ConfigDocument types")
	void exactWritesStampCurrentVersion(@TempDir Path tempDir) {
		Config config = versionedSettingsConfig();
		Path file = tempDir.resolve("written-settings");

		SettingsConfig settings = new SettingsConfig();
		settings.connection.routing.defaults.step.target = "proxy-auth";

		config.write(file, settings);

		JsonNode persisted = config.readNode(file);
		assertEquals(1, persisted.path("_version").asInt());
		assertEquals("proxy-auth", persisted.path("connection").path("routing").path("defaults").path("step").path("target").asText());
	}

	@Test
	@DisplayName("Merge uses the migrated existing tree without rewriting the file")
	void mergeUsesMigratedExistingTree(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("settings.yml");
		Files.writeString(file, """
				connection:
				  routing:
				    defaultProxy: proxy-auth
				""");

		Config config = versionedSettingsConfig();
		SettingsConfig merged = config.merge(file, new SettingsConfig());

		assertEquals(1, merged.getVersion());
		assertEquals("proxy-auth", merged.connection.routing.defaults.step.target);

		JsonNode rawTree = config.readNode(file);
		assertTrue(rawTree.path("_version").isMissingNode());
		assertEquals("proxy-auth", rawTree.path("connection").path("routing").path("defaultProxy").asText());
	}

	@Test
	@DisplayName("Save backs up the existing file before persisting a migrated version")
	void saveBacksUpMigratedFile(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("settings.yml");
		Files.writeString(file, """
				connection:
				  routing:
				    defaultProxy: proxy-auth
				""");

		Config config = versionedSettingsConfig();
		SettingsConfig settings = new SettingsConfig();
		settings.connection.routing.defaults.step.target = "proxy-auth";

		config.save(file, settings);

		Path backup = file.resolveSibling("settings.yml.bak");
		assertTrue(Files.exists(backup));

		String backupContent = Files.readString(backup);
		assertTrue(backupContent.contains("defaultProxy: proxy-auth"));
		assertFalse(backupContent.contains("_version"));
	}

	@Test
	@DisplayName("Duplicate migration source versions are rejected")
	void duplicateMigrationSourceVersionsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> Config.builder()
				.format(Format.YAML)
				.versioned(SettingsConfig.class, spec -> spec
						.currentVersion(1)
						.migration(new SettingsMigrationV0ToV1())
						.migration(new DuplicateSettingsMigrationV0ToV1()))
				.build());
	}

	@Test
	@DisplayName("Broken migration chains are rejected")
	void brokenMigrationChainsAreRejected() {
		assertThrows(ConfigException.class, () -> Config.builder()
				.format(Format.YAML)
				.versioned(ProfileConfig.class, spec -> spec
						.currentVersion(2)
						.migration(new ProfileMigrationV0ToV1()))
				.build());
	}

	@Test
	@DisplayName("Future config versions fail fast")
	void futureConfigVersionsFailFast(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("settings.yml");
		Files.writeString(file, """
				_version: 5
				connection:
				  routing: {}
				""");

		Config config = versionedSettingsConfig();

		assertThrows(ConfigException.class, () -> config.read(file, SettingsConfig.class));
	}

	@Test
	@DisplayName("Invalid version field types fail fast")
	void invalidVersionFieldTypesFailFast(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("settings.yml");
		Files.writeString(file, """
				_version: legacy
				connection:
				  routing: {}
				""");

		Config config = versionedSettingsConfig();

		assertThrows(ConfigException.class, () -> config.read(file, SettingsConfig.class));
	}

	@Test
	@DisplayName("Missing version stays null on models without a persisted field")
	void missingVersionStaysNullOnModelWithoutPersistedField(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("plain.yml");
		Files.writeString(file, """
				connection:
				  routing:
				    defaultProxy: proxy-auth
				""");

		Config config = versionedPlainSettingsConfig();
		PlainSettingsConfig settings = config.read(file, PlainSettingsConfig.class);

		assertEquals("proxy-auth", settings.connection.routing.defaults.step.target);
		assertNull(config.readMigratedNode(file, PlainSettingsConfig.class).get("_version"));
	}

	private Config versionedSettingsConfig() {
		return Config.builder()
				.format(Format.YAML)
				.versioned(SettingsConfig.class, spec -> spec
						.currentVersion(1)
						.migration(new SettingsMigrationV0ToV1()))
				.build();
	}

	private Config versionedCustomVersionConfig() {
		return Config.builder()
				.format(Format.YAML)
				.versioned(CustomVersionConfig.class, spec -> spec
						.currentVersion(1)
						.migration(new CustomVersionMigrationV0ToV1()))
				.build();
	}

	private Config versionedPlainSettingsConfig() {
		return Config.builder()
				.format(Format.YAML)
				.versioned(PlainSettingsConfig.class, spec -> spec
						.currentVersion(1)
						.migration(new PlainSettingsMigrationV0ToV1()))
				.build();
	}

	private Config versionedFallbackConfig() {
		return Config.builder()
				.format(Format.YAML)
				.versioned(FallbackProfileConfig.class, spec -> spec
						.currentVersion(2)
						.migration(new FallbackProfileMigrationV0ToV1())
						.migration(new FallbackProfileMigrationV1ToV2()))
				.build();
	}

	static final class SettingsConfig extends ConfigDocument {
		public Connection connection = new Connection();

		static final class Connection {
			public Routing routing = new Routing();
		}

		static final class Routing {
			public Defaults defaults = new Defaults();
		}

		static final class Defaults {
			public Target step = new Target();
		}

		static final class Target {
			public String target;
		}
	}

	static final class PlainSettingsConfig {
		public Connection connection = new Connection();

		static final class Connection {
			public Routing routing = new Routing();
		}

		static final class Routing {
			public Defaults defaults = new Defaults();
		}

		static final class Defaults {
			public Target step = new Target();
		}

		static final class Target {
			public String target;
		}
	}

	static final class CustomVersionConfig {
		@SchemaVersion
		@JsonProperty("schemaVersion")
		public Integer schemaVersion;
		public String label;
	}

	static final class ProfileConfig extends ConfigDocument {
		public Profile profile = new Profile();

		static final class Profile {
			public String displayName;
		}
	}

	static final class FallbackProfileConfig {
		public Profile profile = new Profile();

		static final class Profile {
			public String displayName;
		}
	}

	static final class SettingsMigrationV0ToV1 implements ConfigMigrationStep<SettingsConfig> {
		@Override
		public Class<SettingsConfig> type() {
			return SettingsConfig.class;
		}

		@Override
		public int fromVersion() {
			return 0;
		}

		@Override
		public int toVersion() {
			return 1;
		}

		@Override
		public ObjectNode migrate(ObjectNode root, ConfigMigrationContext context) {
			moveDefaultProxy(root, context);
			return root;
		}
	}

	static final class PlainSettingsMigrationV0ToV1 implements ConfigMigrationStep<PlainSettingsConfig> {
		@Override
		public Class<PlainSettingsConfig> type() {
			return PlainSettingsConfig.class;
		}

		@Override
		public int fromVersion() {
			return 0;
		}

		@Override
		public int toVersion() {
			return 1;
		}

		@Override
		public ObjectNode migrate(ObjectNode root, ConfigMigrationContext context) {
			moveDefaultProxy(root, context);
			return root;
		}
	}

	static final class CustomVersionMigrationV0ToV1 implements ConfigMigrationStep<CustomVersionConfig> {
		@Override
		public Class<CustomVersionConfig> type() {
			return CustomVersionConfig.class;
		}

		@Override
		public int fromVersion() {
			return 0;
		}

		@Override
		public int toVersion() {
			return 1;
		}

		@Override
		public ObjectNode migrate(ObjectNode root, ConfigMigrationContext context) {
			return root;
		}
	}

	static final class DuplicateSettingsMigrationV0ToV1 implements ConfigMigrationStep<SettingsConfig> {
		@Override
		public Class<SettingsConfig> type() {
			return SettingsConfig.class;
		}

		@Override
		public int fromVersion() {
			return 0;
		}

		@Override
		public int toVersion() {
			return 1;
		}

		@Override
		public ObjectNode migrate(ObjectNode root, ConfigMigrationContext context) {
			return root;
		}
	}

	static final class ProfileMigrationV0ToV1 implements ConfigMigrationStep<ProfileConfig> {
		@Override
		public Class<ProfileConfig> type() {
			return ProfileConfig.class;
		}

		@Override
		public int fromVersion() {
			return 0;
		}

		@Override
		public int toVersion() {
			return 1;
		}

		@Override
		public ObjectNode migrate(ObjectNode root, ConfigMigrationContext context) {
			JsonNode name = root.remove("name");
			if (name != null && !name.isNull())
				root.set("displayName", name);
			return root;
		}
	}

	static final class ProfileMigrationV1ToV2 implements ConfigMigrationStep<ProfileConfig> {
		@Override
		public Class<ProfileConfig> type() {
			return ProfileConfig.class;
		}

		@Override
		public int fromVersion() {
			return 1;
		}

		@Override
		public int toVersion() {
			return 2;
		}

		@Override
		public ObjectNode migrate(ObjectNode root, ConfigMigrationContext context) {
			JsonNode displayName = root.remove("displayName");
			ObjectNode profile = context.object(root, "profile");
			if (displayName != null && !displayName.isNull())
				profile.set("displayName", displayName);
			return root;
		}
	}

	static final class FallbackProfileMigrationV0ToV1 implements ConfigMigrationStep<FallbackProfileConfig> {
		@Override
		public Class<FallbackProfileConfig> type() {
			return FallbackProfileConfig.class;
		}

		@Override
		public int fromVersion() {
			return 0;
		}

		@Override
		public int toVersion() {
			return 1;
		}

		@Override
		public ObjectNode migrate(ObjectNode root, ConfigMigrationContext context) {
			return root;
		}
	}

	static final class FallbackProfileMigrationV1ToV2 implements ConfigMigrationStep<FallbackProfileConfig> {
		@Override
		public Class<FallbackProfileConfig> type() {
			return FallbackProfileConfig.class;
		}

		@Override
		public int fromVersion() {
			return 1;
		}

		@Override
		public int toVersion() {
			return 2;
		}

		@Override
		public ObjectNode migrate(ObjectNode root, ConfigMigrationContext context) {
			JsonNode label = root.remove("label");
			ObjectNode profile = context.object(root, "profile");
			if (label != null && !label.isNull())
				profile.set("displayName", label);
			return root;
		}
	}

	private static void moveDefaultProxy(ObjectNode root, ConfigMigrationContext context) {
		ObjectNode connection = context.object(root, "connection");
		ObjectNode routing = context.object(connection, "routing");

		JsonNode defaultProxy = routing.remove("defaultProxy");
		if (defaultProxy != null && !defaultProxy.isNull()) {
			ObjectNode defaults = context.object(routing, "defaults");
			ObjectNode step = context.object(defaults, "step");
			step.set("target", defaultProxy);
		}
	}
}
