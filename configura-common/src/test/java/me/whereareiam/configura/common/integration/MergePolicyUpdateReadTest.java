package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.Policy;
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

	static class PerFieldPolicy {
		@Field
		public String safeToMerge;

		@Field
		@Policy(mergeOnUpdate = false)
		public String keepAsIs;
	}

	@Test
	void fieldPolicyKeepExistingPreservesValue(@TempDir Path dir) throws Exception {
		String base = dir.resolve("fieldpolicy").toString();

		PerFieldPolicy existing = new PerFieldPolicy();
		existing.safeToMerge = "updated";
		existing.keepAsIs = "original";
		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		writer.encode(base, existing);

		// Read file and manually edit (robust to quoted/unquoted rendering)
		Path yaml = Path.of(base + ".yml");
		String content = Files.readString(yaml);
		content = content.replace("keepAsIs: original", "keepAsIs: preserved");
		content = content.replace("keepAsIs: \"original\"", "keepAsIs: preserved");
		Files.writeString(yaml, content, StandardCharsets.UTF_8);

		// Now try to update with new model values
		PerFieldPolicy model = new PerFieldPolicy();
		model.safeToMerge = "newValue";
		model.keepAsIs = "shouldNotAppear";

		writer.encode(base, model);
		PerFieldPolicy result = new DefaultConfigReader().withFormat(Format.YAML).load(base, PerFieldPolicy.class);

		assertEquals("newValue", result.safeToMerge); // merged
		assertEquals("preserved", result.keepAsIs); // kept as-is

		// Verify file content contains preserved value
		String updated = Files.readString(yaml);
		assertFalse(updated.contains("keepAsIs: original") || updated.contains("keepAsIs: \"original\""));
	}

	@Test
	void fieldPolicyWithNoExistingWritesDefault(@TempDir Path dir) {
		String base = dir.resolve("fieldpolicy2").toString();

		PerFieldPolicy model = new PerFieldPolicy();
		model.safeToMerge = "default";
		model.keepAsIs = "seed";

		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		writer.encode(base, model);
		PerFieldPolicy result = new DefaultConfigReader().withFormat(Format.YAML).load(base, PerFieldPolicy.class);

		assertEquals("default", result.safeToMerge);
		assertEquals("seed", result.keepAsIs);
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


