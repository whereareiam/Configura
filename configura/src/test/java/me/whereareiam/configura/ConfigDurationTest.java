package me.whereareiam.configura;

import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Config Duration")
class ConfigDurationTest {
	@Test
	@DisplayName("Writes readable duration strings by default")
	void writesReadableDurationStringsByDefault(@TempDir Path tempDir) throws Exception {
		Config config = Config.builder()
				.format(Format.YAML)
				.build();
		DurationConfig value = new DurationConfig();
		value.ttl = Duration.ofHours(2).plusMinutes(30);

		Path path = tempDir.resolve("duration.yml");
		config.write(path, value);

		String content = Files.readString(path);
		assertTrue(content.contains("ttl: \"2h30m\"") || content.contains("ttl: 2h30m"));
		assertEquals(value.ttl, config.read(path, DurationConfig.class).ttl);
	}

	@Test
	@DisplayName("Reads human readable and ISO duration strings")
	void readsHumanReadableAndIsoDurationStrings(@TempDir Path tempDir) throws Exception {
		Config config = Config.builder()
				.format(Format.YAML)
				.build();
		Path path = tempDir.resolve("duration.yml");

		Files.writeString(path, "ttl: 15m\n");
		assertEquals(Duration.ofMinutes(15), config.read(path, DurationConfig.class).ttl);

		Files.writeString(path, "ttl: PT30S\n");
		assertEquals(Duration.ofSeconds(30), config.read(path, DurationConfig.class).ttl);
	}

	@Test
	@DisplayName("Keeps bare numbers as minutes")
	void keepsBareNumbersAsMinutes(@TempDir Path tempDir) throws Exception {
		Config config = Config.builder()
				.format(Format.YAML)
				.build();
		Path path = tempDir.resolve("duration.yml");

		Files.writeString(path, "ttl: 15\n");

		assertEquals(Duration.ofMinutes(15), config.read(path, DurationConfig.class).ttl);
	}

	public static class DurationConfig {
		public Duration ttl;
	}
}
