package me.whereareiam.configura.common.processor;

import me.whereareiam.configura.annotation.PostProcess;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PostProcessIntegrationTest {
	@TempDir
	Path tempDir;

	static class Settings {
		public int level;

		public boolean enabled;

		public transient boolean wasProcessed = false;
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

		public transient boolean validated = false;

		@PostProcess
		public void validate() {
			if (port < 0 || port > 65535) {
				throw new IllegalStateException("Port must be between 0 and 65535");
			}
			if (host == null || host.isEmpty()) {
				throw new IllegalStateException("Host cannot be empty");
			}
			validated = true;
		}
	}

	static class CacheConfig {
		public static int instanceCount = 0;

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

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		Settings settings = reader.load(yaml.getBytes(), Settings.class);

		assertTrue(settings.wasProcessed);
		assertEquals(5, settings.level);
		assertEquals(50, settings.computedValue);
	}

	@Test
	void postProcessIsCalledAfterLoadingFromFile() {
		Settings toWrite = new Settings();
		toWrite.level = 3;
		toWrite.enabled = false;

		Path configFile = tempDir.resolve("settings.yml");
		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		writer.encode(configFile, toWrite);

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		Settings settings = reader.load(configFile, Settings.class);

		assertTrue(settings.wasProcessed);
		assertEquals(3, settings.level);
		assertEquals(30, settings.computedValue);
	}

	@Test
	void postProcessIsCalledOnEmptyConfig() {
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		Settings settings = reader.load(new byte[0], Settings.class);

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

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		ValidationConfig config = reader.load(validYaml.getBytes(), ValidationConfig.class);

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

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);

		Exception exception = assertThrows(Exception.class, () -> {
			reader.load(invalidYaml.getBytes(), ValidationConfig.class);
		});

		// The validation exception gets wrapped in ConfigException
		Throwable rootCause = exception;
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		
		String message = rootCause.getMessage();
		assertTrue(message != null && message.contains("Port must be between"), 
				"Expected validation error but got: " + message);
	}

	@Test
	void postProcessWithOnceIsCalledOnlyOnceAcrossMultipleLoads() {
		String yaml1 = "size: 100";
		String yaml2 = "size: 200";

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);

		CacheConfig config1 = reader.load(yaml1.getBytes(), CacheConfig.class);
		assertEquals(1, CacheConfig.instanceCount);

		CacheConfig config2 = reader.load(yaml2.getBytes(), CacheConfig.class);
		// Still 1 because once = true
		assertEquals(1, CacheConfig.instanceCount);

		assertEquals(100, config1.size);
		assertEquals(200, config2.size);
	}

	@Test
	void postProcessIsCalledAfterReadingMergedConfig() {
		Path configFile = tempDir.resolve("settings.yml");

		// Create initial config file
		Settings toWrite = new Settings();
		toWrite.level = 7;
		toWrite.enabled = true;
		
		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML);
		writer.encode(configFile, toWrite);

		// Read the config - PostProcess should be called
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		Settings loaded = reader.load(configFile, Settings.class);

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
			fullName = (firstName != null ? firstName : "") + " " +
					(lastName != null ? lastName : "");
			fullName = fullName.trim();
		}
	}

	@Test
	void postProcessCanComputeDerivedFields() {
		String yaml = """
				firstName: John
				lastName: Doe
				""";

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		DerivedStateConfig config = reader.load(yaml.getBytes(), DerivedStateConfig.class);

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
		String yaml = "value: 42";

		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML);
		MultiStageConfig config = reader.load(yaml.getBytes(), MultiStageConfig.class);

		assertEquals("Stage1:42", config.stage1Result);
		assertEquals("Stage2:42", config.stage2Result);
	}
}
