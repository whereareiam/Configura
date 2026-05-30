package me.whereareiam.configura.feature.postprocess;


import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.postprocess.api.PostProcess;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PostProcessIntegrationTest {
	@TempDir
	Path tempDir;

	static class Settings {
		public int level;
		public boolean enabled;
		public transient boolean wasProcessed;
		public transient int computedValue;

		@PostProcess
		public void afterLoad() {
			wasProcessed = true;
			computedValue = level * 10;
		}
	}

	static class ValidationConfig {
		public int port;
		public String host;
		public transient boolean validated;

		@PostProcess
		public void validate() {
			if (port < 0 || port > 65535)
				throw new IllegalStateException("Port must be between 0 and 65535");
			if (host == null || host.isEmpty())
				throw new IllegalStateException("Host cannot be empty");
			validated = true;
		}
	}

	static class CacheConfig {
		public static int instanceCount;
		public int size;

		@PostProcess(once = true)
		public void initializeCache() {
			instanceCount++;
		}

		public static void resetCount() {
			instanceCount = 0;
		}
	}

	@BeforeEach
	void setUp() {
		CacheConfig.resetCount();
	}

	@Test
	void postProcessIsCalledAfterLoadingFromBytes() {
		String yaml = """
				level: 5
				enabled: true
				""";

		Settings settings = reader().read(yaml.getBytes(), Settings.class);

		assertTrue(settings.wasProcessed);
		assertEquals(5, settings.level);
		assertEquals(50, settings.computedValue);
	}

	@Test
	void postProcessIsCalledAfterLoadingFromFile() throws Exception {
		Path configFile = tempDir.resolve("settings.yml");
		Files.writeString(configFile, "level: 3\nenabled: false\n");

		Settings settings = reader().read(configFile, Settings.class);

		assertTrue(settings.wasProcessed);
		assertEquals(3, settings.level);
		assertEquals(30, settings.computedValue);
	}

	@Test
	void postProcessIsCalledOnEmptyConfig() {
		Settings settings = reader().read(new byte[0], Settings.class);

		assertTrue(settings.wasProcessed);
		assertEquals(0, settings.level);
		assertEquals(0, settings.computedValue);
	}

	@Test
	void postProcessCanPerformValidation() {
		String validYaml = """
				port: 8080
				host: localhost
				""";

		ValidationConfig config = reader().read(validYaml.getBytes(), ValidationConfig.class);

		assertTrue(config.validated);
		assertEquals(8080, config.port);
		assertEquals("localhost", config.host);
	}

	@Test
	void postProcessValidationThrowsExceptionForInvalidData() {
		String invalidYaml = """
				port: 99999
				host: localhost
				""";

		Exception exception = assertThrows(Exception.class, () -> reader().read(invalidYaml.getBytes(), ValidationConfig.class));

		Throwable rootCause = exception;
		while (rootCause.getCause() != null)
			rootCause = rootCause.getCause();

		String message = rootCause.getMessage();
		assertTrue(message != null && message.contains("Port must be between"),
				"Expected validation error but got: " + message);
	}

	@Test
	void postProcessWithOnceIsCalledOnlyOnceAcrossMultipleLoads() {
		Configura reader = reader();
		CacheConfig config1 = reader.read("size: 100".getBytes(), CacheConfig.class);
		assertEquals(1, CacheConfig.instanceCount);

		CacheConfig config2 = reader.read("size: 200".getBytes(), CacheConfig.class);
		assertEquals(1, CacheConfig.instanceCount);

		assertEquals(100, config1.size);
		assertEquals(200, config2.size);
	}

	@Test
	void postProcessIsCalledAfterReadingMergedConfig() throws Exception {
		Path configFile = tempDir.resolve("settings.yml");
		Files.writeString(configFile, "level: 7\nenabled: true\n");

		Settings loaded = reader().read(configFile, Settings.class);

		assertTrue(loaded.wasProcessed);
		assertEquals(7, loaded.level);
		assertEquals(70, loaded.computedValue);
	}

	static class DerivedStateConfig {
		public String firstName;
		public String lastName;
		public transient String fullName;

		@PostProcess
		public void buildDerivedState() {
			fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
		}
	}

	@Test
	void postProcessCanComputeDerivedFields() {
		String yaml = """
				firstName: John
				lastName: Doe
				""";

		DerivedStateConfig config = reader().read(yaml.getBytes(), DerivedStateConfig.class);

		assertEquals("John", config.firstName);
		assertEquals("Doe", config.lastName);
		assertEquals("John Doe", config.fullName);
	}

	static class MultiStageConfig {
		public int value;
		public transient String stage1Result;
		public transient String stage2Result;

		@PostProcess
		public void stage1() {
			stage1Result = "Stage1:" + value;
		}

		@PostProcess
		public void stage2() {
			stage2Result = "Stage2:" + value;
		}
	}

	@Test
	void multiplePostProcessMethodsAreAllExecuted() {
		MultiStageConfig config = reader().read("value: 42".getBytes(), MultiStageConfig.class);

		assertEquals("Stage1:42", config.stage1Result);
		assertEquals("Stage2:42", config.stage2Result);
	}

	private Configura reader() {
		return Config.builder()
				.format(Format.YAML)
				.feature(PostProcessFeature.defaults())
				.build();
	}
}
