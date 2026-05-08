package me.whereareiam.configura.common.util;

import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileUtilTest {
	@Test
	void appendsFormatExtensionWhenMissing() {
		assertEquals(Path.of("config.yml"), FileUtil.resolvePathWithFormat("config", Format.YAML));
		assertEquals(Path.of("config.json"), FileUtil.resolvePathWithFormat(Path.of("config"), Format.JSON));
	}

	@Test
	void preservesRecognizedConfigExtensions() {
		assertEquals(Path.of("config.yml"), FileUtil.resolvePathWithFormat("config.yml", Format.JSON));
		assertEquals(Path.of("config.yaml"), FileUtil.resolvePathWithFormat("config.yaml", Format.YAML));
		assertEquals(Path.of("config.json"), FileUtil.resolvePathWithFormat(Path.of("config.json"), Format.YAML));
	}

	@Test
	void preservesRequestedCustomExtension() {
		assertEquals(Path.of("config.toml"), FileUtil.resolvePathWithExtension("config.toml", ".toml"));
		assertEquals(Path.of("config.toml"), FileUtil.resolvePathWithExtension(Path.of("config"), "toml"));
	}
}
