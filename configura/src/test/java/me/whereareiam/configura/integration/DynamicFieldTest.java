package me.whereareiam.configura.integration;

import lombok.Getter;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DynamicFieldTest {
	@Getter
	static class DynamicConfig {
		@Field(dynamic = true)
		private final Map<String, Object> entries = new LinkedHashMap<>();
	}

	@Test
	void dynamicFieldsCaptureAndWrite(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("dynamic.yml");

		Files.writeString(file, "alpha: 1\nbeta:\n  text: \"Hello\"\n");

		DynamicConfig loaded = Config.load(file, DynamicConfig.class);
		assertEquals(1, ((Number) loaded.getEntries().get("alpha")).intValue());
		assertInstanceOf(Map.class, loaded.getEntries().get("beta"));

		DynamicConfig out = new DynamicConfig();
		out.getEntries().put("foo", "bar");
		out.getEntries().put("nested", Map.of("value", "x"));
		Config.getDefaultWriter().write(file, out);

		@SuppressWarnings("unchecked")
		Map<String, Object> raw = (Map<String, Object>) Config.load(file, Map.class);
		assertFalse(raw.containsKey("entries"));
		assertEquals("bar", raw.get("foo"));
		assertInstanceOf(Map.class, raw.get("nested"));
	}
}
