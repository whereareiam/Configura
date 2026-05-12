package me.whereareiam.configura;

import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.annotation.PostProcess;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.configura.merge.strategy.type.DeclaredKeysOnlyMap;
import me.whereareiam.configura.merge.strategy.type.StructuralObject;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Config Integration")
class ConfigIntegrationTest {
	@Test
	@DisplayName("Update applies defaults and runs post-process hooks")
	void updateAppliesDefaultsAndRunsPostProcess(@TempDir Path tempDir) {
		Config configura = Config.builder()
				.format(Format.YAML)
				.defaults(BasicDefaults.class)
				.build();

		BasicConfig config = configura.update(tempDir.resolve("basic"), BasicConfig.class);

		assertEquals("service", config.name);
		assertTrue(config.processed);
	}

	@Test
	@DisplayName("Explicit null wins globally during update")
	void explicitNullWinsGloballyDuringUpdate(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(Format.YAML)
				.defaults(NullDefaults.class)
				.build();

		Path file = tempDir.resolve("nulls.yml");
		Files.writeString(file, "value: null\n");

		NullConfig config = configura.update(file, NullConfig.class);
		assertNull(config.value);
		assertTrue(configura.readNode(file).get("value").isNull());
	}

	@Test
	@DisplayName("Present keys merge only declared map entries")
	void presentKeysMergeOnlyDeclaredMapEntries(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(Format.YAML)
				.defaults(RoutingDefaults.class)
				.build();

		Path file = tempDir.resolve("routing.yml");
		Files.writeString(file, """
				routing:
				  scenarios:
				    authentication:
				      complete: ""
				""");

		RoutingConfig config = configura.update(file, RoutingConfig.class);
		assertNotNull(config.routing);
		assertEquals(1, config.routing.scenarios.size());
		assertTrue(config.routing.scenarios.containsKey("authentication"));
		assertEquals("auth", config.routing.scenarios.get("authentication").step);
		assertEquals("", config.routing.scenarios.get("authentication").complete);

		String persisted = Files.readString(file);
		assertTrue(persisted.contains("authentication:"));
		assertFalse(persisted.contains("registration:"));
	}

	@Test
	@DisplayName("Default merge strategy applies to unannotated fields")
	void defaultMergeStrategyAppliesToUnannotatedFields(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(Format.YAML)
				.defaults(DefaultRoutingDefaults.class)
				.defaultMergeStrategy(DeclaredKeysOnlyMap.class)
				.build();

		Path file = tempDir.resolve("routing-default.yml");
		Files.writeString(file, """
				scenarios:
				  authentication:
				    complete: ""
				""");

		DefaultRoutingConfig config = configura.update(file, DefaultRoutingConfig.class);
		assertEquals(1, config.scenarios.size());
		assertTrue(config.scenarios.containsKey("authentication"));
		assertFalse(config.scenarios.containsKey("registration"));
	}

	@Test
	@DisplayName("Named merge strategies can be registered and selected")
	void namedMergeStrategiesCanBeRegisteredAndSelected(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(Format.YAML)
				.defaults(NamedRoutingDefaults.class)
				.mergeStrategy("declaredKeysOnly", DeclaredKeysOnlyMap.class)
				.build();

		Path file = tempDir.resolve("routing-custom.yml");
		Files.writeString(file, """
				routing:
				  scenarios:
				    authentication:
				      complete: ""
				""");

		NamedRoutingConfig config = configura.update(file, NamedRoutingConfig.class);
		assertNotNull(config.routing);
		assertEquals(1, config.routing.scenarios.size());
		assertTrue(config.routing.scenarios.containsKey("authentication"));
		assertFalse(config.routing.scenarios.containsKey("registration"));
	}

	@Test
	@DisplayName("Structural object strategy restores the object without filling declared children")
	void structuralObjectRestoresObjectWithoutFillingDeclaredChildren(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(Format.YAML)
				.defaults(ProviderDefaults.class)
				.build();

		Path missingOverrides = tempDir.resolve("provider-missing-overrides.yml");
		Files.writeString(missingOverrides, """
				provider:
				  id: premium
				""");

		ProviderConfig restored = configura.update(missingOverrides, ProviderConfig.class);
		assertNotNull(restored.provider.overrides);
		assertEquals("12h", restored.provider.overrides.sessionTtl);

		Path declaredOverrides = tempDir.resolve("provider-declared-overrides.yml");
		Files.writeString(declaredOverrides, """
				provider:
				  id: premium
				  overrides: {}
				""");

		ProviderConfig declared = configura.update(declaredOverrides, ProviderConfig.class);
		assertNotNull(declared.provider.overrides);
		assertNull(declared.provider.overrides.sessionTtl);

		String persisted = Files.readString(declaredOverrides);
		assertTrue(persisted.contains("overrides: {}"));
		assertFalse(persisted.contains("sessionTtl"));
	}

	@Test
	@DisplayName("Duplicate YAML keys fail fast during update")
	void duplicateYamlKeysFailFastDuringUpdate(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(Format.YAML)
				.build();

		Path file = tempDir.resolve("duplicate.yml");
		Files.writeString(file, """
				scenario:
				  registration:
				    requireRepeat: false
				scenario:
				  registration:
				    requireRepeat: true
				""");

		assertThrows(ConfigException.class, () -> configura.update(file, DuplicateScenarioConfig.class));
	}

	@Test
	@DisplayName("Explicit false survives update defaults merge")
	void explicitFalseSurvivesUpdateDefaultsMerge(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(me.whereareiam.configura.type.Format.YAML)
				.defaults(BooleanDefaults.class)
				.build();

		Path file = tempDir.resolve("boolean.yml");
		Files.writeString(file, "enabled: false\n");

		BooleanConfig config = configura.update(file, BooleanConfig.class);
		assertFalse(config.enabled);
		assertTrue(Files.readString(file).contains("enabled: false"));
	}

	@Test
	@DisplayName("Explicit zero survives update defaults merge")
	void explicitZeroSurvivesUpdateDefaultsMerge(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(me.whereareiam.configura.type.Format.YAML)
				.defaults(NumberDefaults.class)
				.build();

		Path file = tempDir.resolve("number.yml");
		Files.writeString(file, "retries: 0\n");

		NumberConfig config = configura.update(file, NumberConfig.class);
		assertEquals(0, config.retries);
		assertTrue(Files.readString(file).contains("retries: 0"));
	}

	@Test
	@DisplayName("Nested explicit false survives update defaults merge")
	void nestedExplicitFalseSurvivesUpdateDefaultsMerge(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(me.whereareiam.configura.type.Format.YAML)
				.defaults(NestedBooleanDefaults.class)
				.build();

		Path file = tempDir.resolve("nested-boolean.yml");
		Files.writeString(file, """
				registration:
				  requireRepeat: false
				""");

		NestedBooleanConfig config = configura.update(file, NestedBooleanConfig.class);
		assertNotNull(config.registration);
		assertFalse(config.registration.requireRepeat);
		assertTrue(Files.readString(file).contains("requireRepeat: false"));
	}

	@Test
	@DisplayName("Nested explicit zero survives update defaults merge")
	void nestedExplicitZeroSurvivesUpdateDefaultsMerge(@TempDir Path tempDir) throws Exception {
		Config configura = Config.builder()
				.format(me.whereareiam.configura.type.Format.YAML)
				.defaults(NestedNumberDefaults.class)
				.build();

		Path file = tempDir.resolve("nested-number.yml");
		Files.writeString(file, """
				limits:
				  retries: 0
				""");

		NestedNumberConfig config = configura.update(file, NestedNumberConfig.class);
		assertNotNull(config.limits);
		assertEquals(0, config.limits.retries);
		assertTrue(Files.readString(file).contains("retries: 0"));
	}

	public static class BasicConfig {
		public String name;
		public transient boolean processed;

		@PostProcess
		public void afterLoad() {
			processed = true;
		}
	}

	public static class BasicDefaults implements MergeDefaultsProvider<BasicConfig> {
		@Override
		public BasicConfig supply(BasicConfig config) {
			config.name = "service";
			return config;
		}
	}

	public static class NullConfig {
		public String value;
	}

	public static class NullDefaults implements MergeDefaultsProvider<NullConfig> {
		@Override
		public NullConfig supply(NullConfig config) {
			config.value = "default";
			return config;
		}
	}

	public static class RoutingConfig {
		public Routing routing;

		public static class Routing {
			@Merge(DeclaredKeysOnlyMap.class)
			public Map<String, Scenario> scenarios = new LinkedHashMap<>();
		}

		public static class Scenario {
			public String step;
			public String complete;
		}
	}

	public static class RoutingDefaults implements MergeDefaultsProvider<RoutingConfig> {
		@Override
		public RoutingConfig supply(RoutingConfig config) {
			config.routing = new RoutingConfig.Routing();

			RoutingConfig.Scenario authentication = new RoutingConfig.Scenario();
			authentication.step = "auth";
			authentication.complete = "lobby";
			config.routing.scenarios.put("authentication", authentication);

			RoutingConfig.Scenario registration = new RoutingConfig.Scenario();
			registration.step = "register";
			registration.complete = "lobby";
			config.routing.scenarios.put("registration", registration);
			return config;
		}
	}

	public static class DefaultRoutingConfig {
		public Map<String, Scenario> scenarios = new LinkedHashMap<>();

		public static class Scenario {
			public String step;
			public String complete;
		}
	}

	public static class DefaultRoutingDefaults implements MergeDefaultsProvider<DefaultRoutingConfig> {
		@Override
		public DefaultRoutingConfig supply(DefaultRoutingConfig config) {
			DefaultRoutingConfig.Scenario authentication = new DefaultRoutingConfig.Scenario();
			authentication.step = "auth";
			authentication.complete = "lobby";
			config.scenarios.put("authentication", authentication);

			DefaultRoutingConfig.Scenario registration = new DefaultRoutingConfig.Scenario();
			registration.step = "register";
			registration.complete = "lobby";
			config.scenarios.put("registration", registration);
			return config;
		}
	}

	public static class NamedRoutingConfig {
		public NamedRouting routing;

		public static class NamedRouting {
				@Merge(named = "declaredKeysOnly")
				public Map<String, Scenario> scenarios = new LinkedHashMap<>();
		}

		public static class Scenario {
			public String step;
			public String complete;
		}
	}

	public static class DuplicateScenarioConfig {
		public Scenario scenario;

		public static class Scenario {
			public Registration registration;
		}

		public static class Registration {
			public boolean requireRepeat;
		}
	}

	public static class BooleanConfig {
		public boolean enabled;
	}

	public static class BooleanDefaults implements MergeDefaultsProvider<BooleanConfig> {
		@Override
		public BooleanConfig supply(BooleanConfig config) {
			config.enabled = true;
			return config;
		}
	}

	public static class NumberConfig {
		public int retries;
	}

	public static class NumberDefaults implements MergeDefaultsProvider<NumberConfig> {
		@Override
		public NumberConfig supply(NumberConfig config) {
			config.retries = 3;
			return config;
		}
	}

	public static class NestedBooleanConfig {
		public Registration registration;

		public static class Registration {
			public boolean requireRepeat;
		}
	}

	public static class NestedBooleanDefaults implements MergeDefaultsProvider<NestedBooleanConfig> {
		@Override
		public NestedBooleanConfig supply(NestedBooleanConfig config) {
			config.registration = new NestedBooleanConfig.Registration();
			config.registration.requireRepeat = true;
			return config;
		}
	}

	public static class NestedNumberConfig {
		public Limits limits;

		public static class Limits {
			public int retries;
		}
	}

	public static class NestedNumberDefaults implements MergeDefaultsProvider<NestedNumberConfig> {
		@Override
		public NestedNumberConfig supply(NestedNumberConfig config) {
			config.limits = new NestedNumberConfig.Limits();
			config.limits.retries = 3;
			return config;
		}
	}

	public static class NamedRoutingDefaults implements MergeDefaultsProvider<NamedRoutingConfig> {
		@Override
		public NamedRoutingConfig supply(NamedRoutingConfig config) {
			config.routing = new NamedRoutingConfig.NamedRouting();

			NamedRoutingConfig.Scenario authentication = new NamedRoutingConfig.Scenario();
			authentication.step = "auth";
			authentication.complete = "lobby";
			config.routing.scenarios.put("authentication", authentication);

			NamedRoutingConfig.Scenario registration = new NamedRoutingConfig.Scenario();
			registration.step = "register";
			registration.complete = "lobby";
			config.routing.scenarios.put("registration", registration);
			return config;
		}
	}

	public static class ProviderConfig {
		public Provider provider;
	}

	public static class Provider {
		public String id;
		@Merge(StructuralObject.class)
		public Overrides overrides = new Overrides();
	}

	public static class Overrides {
		public String sessionTtl;
	}

	public static class ProviderDefaults implements MergeDefaultsProvider<ProviderConfig> {
		@Override
		public ProviderConfig supply(ProviderConfig config) {
			config.provider = new Provider();
			config.provider.id = "premium";
			config.provider.overrides = new Overrides();
			config.provider.overrides.sessionTtl = "12h";
			return config;
		}
	}
}
