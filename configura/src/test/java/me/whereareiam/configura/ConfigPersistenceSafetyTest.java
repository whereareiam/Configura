package me.whereareiam.configura;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigPersistenceSafetyTest {
	@TempDir Path directory;
	@Test void failedBindingDoesNotOverwriteInput() throws Exception {
		Path path = directory.resolve("settings.yml");
		String original = "count: not-a-number\ncustom: retained\n";
		Files.writeString(path, original);
		assertThrows(RuntimeException.class, () -> Config.yaml().update(path, Settings.class));
		assertEquals(original, Files.readString(path));
	}
	@Test void serializationFailureDoesNotTruncateExistingFile() throws Exception {
		Path path = directory.resolve("settings.yml"); Files.writeString(path, "original: true\n");
		assertThrows(RuntimeException.class, () -> Config.yaml().write(path, new FailingModel()));
		assertEquals("original: true\n", Files.readString(path));
	}
	@Test void preparesDefaultsAndValidatesWithoutWriting() {
		var configura = Config.yaml();
		var tree = configura.mapper().createObjectNode().put("count", 7);
		assertEquals(7, configura.prepareNode(tree, Settings.class).get("count").intValue());
		assertFalse(Files.exists(directory.resolve("settings.yml")));
	}
	public static class Settings {
		public int count;
	}
	public static class FailingModel {
		public String getValue() { throw new IllegalStateException("serialization failed"); }
	}
}
