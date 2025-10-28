package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.Policy;
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
		Config.save(base, existing);

		// Read file and manually edit (robust to quoted/unquoted rendering)
		Path yaml = Path.of(base + ".yaml");
		String content = Files.readString(yaml);
		content = content.replace("keepAsIs: original", "keepAsIs: preserved");
		content = content.replace("keepAsIs: \"original\"", "keepAsIs: preserved");
		Files.writeString(yaml, content, StandardCharsets.UTF_8);

		// Now try to update with new model values
		PerFieldPolicy model = new PerFieldPolicy();
		model.safeToMerge = "newValue";
		model.keepAsIs = "shouldNotAppear";

		PerFieldPolicy result = Config.updateRead(base, model);

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

		PerFieldPolicy result = Config.updateRead(base, model);

		assertEquals("default", result.safeToMerge);
		assertEquals("seed", result.keepAsIs);
	}

	@Test
	void mergeTruePrunesUnknown(@TempDir Path dir) throws Exception {
		String base = dir.resolve("app").toString();

		Old o = new Old();
		o.host = "0.0.0.0";
		o.port = 9000;

		Config.save(base + ".yaml", o);

		Path yaml = Path.of(base + ".yaml");
		Files.writeString(yaml, Files.readString(yaml) + "\nunknown: 1\n", StandardCharsets.UTF_8);

		New n = new New();
		n.host = "0.0.0.0";
		n.port = 9000;

		New after = Config.updateRead(base + ".yaml", n);

		String updated = Files.readString(yaml);
		assertFalse(updated.contains("unknown:"));

		assertEquals("0.0.0.0", after.host);
		assertEquals(9000, after.port);
	}
}


