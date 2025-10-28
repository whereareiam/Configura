package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigFormatAutoDetectionTest {
	static class CounterConfig {
		public int value;
	}

	@Test
	void autodetectYamlAndJson(@TempDir Path dir) {
		CounterConfig counter = new CounterConfig();
		counter.value = 1;

		String y = dir.resolve("x.yaml").toString();
		String j = dir.resolve("x.json").toString();

		Config.save(y, counter);
		Config.save(j, counter);

		assertEquals(1, Config.load(y, CounterConfig.class).value);
		assertEquals(1, Config.load(j, CounterConfig.class).value);
	}
}


