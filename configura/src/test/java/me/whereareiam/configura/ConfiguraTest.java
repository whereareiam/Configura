package me.whereareiam.configura;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.configura.annotation.merge.MergeValue;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.type.Format;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguraTest {
	private final Configura configura = Config.builder()
			.feature(new MarkerFeature())
			.build();

	@TempDir
	Path directory;

	private Path file;

	@BeforeEach
	void writeFile() throws Exception {
		file = directory.resolve("settings.yml");
		Files.writeString(file, "name: hub\n_marker: 7\nunknown: dropped\n");
	}

	@Test
	void reportsTheKeysItsFeaturesReserve() {
		assertEquals(Set.of("_marker"), configura.reservedKeys());
		assertTrue(Config.yaml().reservedKeys().isEmpty());
	}

	@Test
	void updateCarriesReservedKeysToTheTopOfTheFile() {
		Settings settings = configura.update(file, Settings.class);

		assertEquals("hub", settings.name);
		assertEquals(7, configura.readNode(file).path("_marker").asInt());
		assertEquals("_marker", configura.readNode(file).fieldNames().next());
		assertFalse(configura.readNode(file).has("unknown"));
	}

	@Test
	void saveAndWriteCarryReservedKeys() {
		Settings settings = new Settings();
		settings.name = "arena";

		configura.save(file, settings);
		assertEquals(7, configura.readNode(file).path("_marker").asInt());

		configura.write(file, settings);
		assertEquals(7, configura.readNode(file).path("_marker").asInt());
		assertEquals("arena", configura.readNode(file).path("name").asText());
	}

	@Test
	void writesNoReservedKeyTheFileDidNotHave() throws Exception {
		Files.writeString(file, "name: hub\n");

		configura.update(file, Settings.class);
		configura.update(directory.resolve("created.yml"), Settings.class);

		assertFalse(configura.readNode(file).has("_marker"));
		assertFalse(configura.readNode(directory.resolve("created.yml")).has("_marker"));
	}

	@Test
	void dropsKeysNoFeatureReserved() {
		Config.yaml().update(file, Settings.class);

		assertFalse(Config.yaml().readNode(file).has("_marker"));
	}

	@Test
	void derivesVariantsWithoutChangingItself() {
		Module module = new SimpleModule("test");
		Configura base = Config.yaml();

		Configura variant = base.toBuilder()
				.format(Format.JSON)
				.module(module)
				.feature(new MarkerFeature())
				.build();

		assertEquals(".json", variant.extension());
		assertTrue(variant.modules().contains(module));
		assertEquals(Set.of("_marker"), variant.reservedKeys());
		assertEquals(".yml", base.extension());
		assertTrue(base.modules().isEmpty());
		assertTrue(base.reservedKeys().isEmpty());
	}

	@Test
	void takesDefaultsFromAProviderAddedLater() {
		Configura withDefaults = Config.yaml().withDefaults(SettingsDefaults.class);

		assertEquals("from-provider", withDefaults.update(directory.resolve("provided.yml"), Settings.class).motd);
		assertNull(Config.yaml().update(directory.resolve("plain.yml"), Settings.class).motd);
	}

	@Test
	void keepsTheFeaturesOfTheInstanceAFeatureIsAddedTo() {
		Configura both = Config.yaml().withFeature(new MarkerFeature()).withFeature(new OtherFeature());

		assertEquals(Set.of("_marker", "_other"), both.reservedKeys());
	}

	@Test
	void treatsInheritedFieldsLikeDeclaredOnes() throws Exception {
		Path inherited = directory.resolve("inherited.yml");
		Files.writeString(inherited, "note: kept\n");

		Extended extended = Config.yaml().update(inherited, Extended.class);

		assertEquals("kept", extended.note);
		assertEquals("inherited-default", extended.greeting);
		assertEquals("kept", Config.yaml().readNode(inherited).path("note").asText());
		assertEquals("inherited-default", Config.yaml().readNode(inherited).path("greeting").asText());
	}

	public static class Base {
		public String note;

		@MergeValue(text = "inherited-default")
		public String greeting;
	}

	public static class Extended extends Base {
		public String name = "lobby";
	}

	public static class Settings {
		public String name = "lobby";
		public int timeout = 30;
		public String motd;
	}

	public static class SettingsDefaults implements DefaultsProvider<Settings> {
		@Override
		public Settings supply(Settings settings) {
			settings.motd = "from-provider";
			return settings;
		}
	}

	private static final class OtherFeature implements ConfiguraFeature {
		@Override
		public @NotNull Set<String> reservedKeys() {
			return Set.of("_other");
		}
	}

	private static final class MarkerFeature implements ConfiguraFeature {
		@Override
		public @NotNull Set<String> reservedKeys() {
			return Set.of("_marker");
		}
	}
}
