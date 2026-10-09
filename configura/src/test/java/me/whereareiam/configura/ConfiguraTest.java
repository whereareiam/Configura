package me.whereareiam.configura;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

	public static class Settings {
		public String name = "lobby";
		public int timeout = 30;
	}

	private static final class MarkerFeature implements ConfiguraFeature {
		@Override
		public @NotNull Set<String> reservedKeys() {
			return Set.of("_marker");
		}
	}
}
