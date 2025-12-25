package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MergePolicyUpdateReadTest {
	static class Old {
		@Field
		public String host;
		@Field
		public int port;
	}

	static class New {
		@Field
		public String host;
		@Field
		public int port;
	}


	@Test
	void mergeTruePrunesUnknown(@TempDir Path dir) throws Exception {
		String base = dir.resolve("app").toString();

		Old o = new Old();
		o.host = "0.0.0.0";
		o.port = 9000;

		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		writer.encode(base + ".yml", o);

		Path yaml = Path.of(base + ".yml");
		Files.writeString(yaml, Files.readString(yaml) + "\nunknown: 1\n", StandardCharsets.UTF_8);

		New n = new New();
		n.host = "0.0.0.0";
		n.port = 9000;

		writer.encode(base + ".yml", n);
		New after = new DefaultConfigReader().withFormat(Format.YAML).load(base + ".yml", New.class);

		String updated = Files.readString(yaml);
		assertFalse(updated.contains("unknown:"));

		assertEquals("0.0.0.0", after.host);
		assertEquals(9000, after.port);
	}
}


