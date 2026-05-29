package me.whereareiam.configura.merge.tree.property;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.annotation.Merge;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.NeverDefaults;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;
import me.whereareiam.configura.merge.strategy.StructuralObject;
import me.whereareiam.configura.type.Format;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PropertyFieldMergeStrategyIntegrationTest {
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

	private static Configura yaml() {
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

		@Merge(StructuralObject.class)
		@Defaults(properties = {
				@Defaults.Property(name = "host", text = "localhost"),
				@Defaults.Property(name = "port", number = "8080")
		})
		public Nested structural;
	}

	static class Nested {
		public String host;
		public int port;
	}

	static class NamedConfig {
		@Merge(named = "alwaysDefault")
		@Defaults(text = "from-default")
		public String value;
	}

	public static class AlwaysDefault implements FieldMergeStrategy {
		@Override
		public JsonNode merge(@NotNull MergeContext context) {
			return context.getDefaultNode();
		}
	}
}
