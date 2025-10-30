package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
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

		String y = dir.resolve("x.yml").toString();
		String j = dir.resolve("x.json").toString();

		ConfigWriter yamlWriter = new DefaultConfigWriter().withFormat(Format.YAML);
		ConfigWriter jsonWriter = new DefaultConfigWriter().withFormat(Format.JSON);
		yamlWriter.save(y, counter);
		jsonWriter.save(j, counter);

		ConfigReader yamlReader = new DefaultConfigReader().withFormat(Format.YAML);
		ConfigReader jsonReader = new DefaultConfigReader().withFormat(Format.JSON);
		assertEquals(1, yamlReader.load(y, CounterConfig.class).value);
		assertEquals(1, jsonReader.load(j, CounterConfig.class).value);
	}
}


