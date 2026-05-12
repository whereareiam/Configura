package me.whereareiam.configura.merge;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.merge.strategy.*;
import me.whereareiam.configura.merge.strategy.type.DeclaredKeysOnlyMap;
import me.whereareiam.configura.merge.strategy.type.NeverDefaults;
import me.whereareiam.configura.merge.strategy.type.SourceOwnsField;
import me.whereareiam.configura.merge.strategy.type.StructuralObject;
import me.whereareiam.configura.type.Format;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMergeStrategyIntegrationTest {
	@Test
	void deepDefaultsFillsMissingNestedValues(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("deep.yml");
		Files.writeString(file, """
				nested:
				  host: example.com
				""");

		StrategyConfig config = yaml().update(file, StrategyConfig.class);

		assertEquals("example.com", config.nested.host);
		assertEquals(8080, config.nested.port);
	}

	@Test
	void sourceOwnsFieldKeepsDeclaredObject(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("source.yml");
		Files.writeString(file, """
				sourceOwned:
				  host: example.com
				""");

		StrategyConfig config = yaml().update(file, StrategyConfig.class);

		assertEquals("example.com", config.sourceOwned.host);
		assertEquals(0, config.sourceOwned.port);
	}

	@Test
	void neverDefaultsSkipsMissingDefaults(@TempDir Path tempDir) {
		StrategyConfig config = yaml().update(tempDir.resolve("never.yml"), StrategyConfig.class);

		assertNull(config.never);
	}

	@Test
	void declaredKeysOnlyMapMergesOnlySourceKeys(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("declared.yml");
		Files.writeString(file, """
				scenarios:
				  authentication:
				    complete: ""
				""");

		StrategyConfig config = yaml().update(file, StrategyConfig.class);

		assertTrue(config.scenarios.containsKey("authentication"));
		assertEquals("auth", config.scenarios.get("authentication").step);
		assertFalse(config.scenarios.containsKey("registration"));
	}

	@Test
	void structuralObjectKeepsDeclaredObjectEmpty(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("structural.yml");
		Files.writeString(file, """
				structural: {}
				""");

		StrategyConfig config = yaml().update(file, StrategyConfig.class);

		assertNotNull(config.structural);
		assertNull(config.structural.host);
		assertEquals(0, config.structural.port);
	}

	@Test
	void namedCustomStrategyCanBeRegistered(@TempDir Path tempDir) {
		NamedConfig config = Config.builder()
				.format(Format.YAML)
				.mergeStrategy("alwaysDefault", AlwaysDefault.class)
				.build()
				.update(tempDir.resolve("named.yml"), NamedConfig.class);

		assertEquals("from-default", config.value);
	}

	private static Config yaml() {
		return Config.builder().format(Format.YAML).build();
	}

	static class StrategyConfig {
		@Merge
		@Defaults(properties = {
				@Defaults.Property(name = "host", text = "localhost"),
				@Defaults.Property(name = "port", number = "8080")
		})
		public Nested nested;

		@Merge(SourceOwnsField.class)
		@Defaults(properties = {
				@Defaults.Property(name = "host", text = "localhost"),
				@Defaults.Property(name = "port", number = "8080")
		})
		public Nested sourceOwned;

		@Merge(NeverDefaults.class)
		@Defaults(text = "hidden")
		public String never;

		@Merge(DeclaredKeysOnlyMap.class)
		public Map<String, Scenario> scenarios = defaultScenarios();

		@Merge(StructuralObject.class)
		@Defaults(properties = {
				@Defaults.Property(name = "host", text = "localhost"),
				@Defaults.Property(name = "port", number = "8080")
		})
		public Nested structural;

		private static Map<String, Scenario> defaultScenarios() {
			Map<String, Scenario> scenarios = new LinkedHashMap<>();
			Scenario authentication = new Scenario();
			authentication.step = "auth";
			authentication.complete = "lobby";
			scenarios.put("authentication", authentication);
			Scenario registration = new Scenario();
			registration.step = "register";
			registration.complete = "lobby";
			scenarios.put("registration", registration);
			return scenarios;
		}
	}

	static class Nested {
		public String host;
		public int port;
	}

	static class Scenario {
		public String step;
		public String complete;
	}

	static class NamedConfig {
		@Merge(named = "alwaysDefault")
		@Defaults(text = "from-default")
		public String value;
	}

	public static class AlwaysDefault implements MergeStrategy {
		@Override
		public JsonNode merge(@NotNull MergeContext context) {
			return context.getDefaultNode();
		}
	}
}
