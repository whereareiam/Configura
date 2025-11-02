package me.whereareiam.configura.common.processor;

import me.whereareiam.configura.annotation.PostProcess;
import me.whereareiam.configura.exception.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PostProcessorTest {
	static class SimpleConfig {
		public String name;
		public int callCount = 0;

		@PostProcess
		public void initialize() {
			callCount++;
			name = name != null ? name.toUpperCase() : "DEFAULT";
		}
	}

	static class ConfigWithOnceMethod {
		public static int globalInitCount = 0;
		public int instanceCount = 0;

		@PostProcess(once = true)
		public void initializeOnce() {
			globalInitCount++;
		}

		@PostProcess
		public void initializeAlways() {
			instanceCount++;
		}

		public static void resetGlobal() {
			globalInitCount = 0;
		}
	}

	static class ConfigWithMultipleMethods {
		public List<String> executionOrder = new ArrayList<>();

		@PostProcess
		public void first() {
			executionOrder.add("first");
		}

		@PostProcess
		public void second() {
			executionOrder.add("second");
		}

		@PostProcess
		public void third() {
			executionOrder.add("third");
		}
	}

	static class InvalidConfigWithParams {
		@PostProcess
		public void initialize(String param) {
			// Invalid: has parameters
		}
	}

	static class InvalidConfigWithReturn {
		@PostProcess
		public String initialize() {
			return "invalid";
		}
	}

	static class InvalidConfigNotPublic {
		@PostProcess
		private void initialize() {
			// Invalid: not public
		}
	}

	static class ConfigWithInheritance extends BaseConfig {
		public int childCallCount = 0;

		@PostProcess
		public void childInit() {
			childCallCount++;
		}
	}

	static class BaseConfig {
		public int baseCallCount = 0;

		@PostProcess
		public void baseInit() {
			baseCallCount++;
		}
	}

	@Test
	void processCallsAnnotatedMethod() {
		SimpleConfig config = new SimpleConfig();
		config.name = "test";

		PostProcessor.process(config);

		assertEquals("TEST", config.name);
		assertEquals(1, config.callCount);
	}

	@Test
	void processWithNullConfigDoesNothing() {
		assertDoesNotThrow(() -> PostProcessor.process(null));
	}

	@Test
	void processSetsDefaultWhenFieldIsNull() {
		SimpleConfig config = new SimpleConfig();

		PostProcessor.process(config);

		assertEquals("DEFAULT", config.name);
	}

	@Test
	void processCallsMethodEveryTimeWithoutOnce() {
		SimpleConfig config = new SimpleConfig();
		config.name = "test";

		PostProcessor.process(config);
		assertEquals(1, config.callCount);

		PostProcessor.process(config);
		assertEquals(2, config.callCount);
	}

	@Test
	void processWithOnceTrueOnlyCallsOncePerClass() {
		ConfigWithOnceMethod.resetGlobal();

		ConfigWithOnceMethod config1 = new ConfigWithOnceMethod();
		PostProcessor.process(config1);

		assertEquals(1, ConfigWithOnceMethod.globalInitCount);
		assertEquals(1, config1.instanceCount);

		ConfigWithOnceMethod config2 = new ConfigWithOnceMethod();
		PostProcessor.process(config2);

		// once = true method should not be called again
		assertEquals(1, ConfigWithOnceMethod.globalInitCount);
		// but regular method should be called
		assertEquals(1, config2.instanceCount);
	}

	@Test
	void processCallsMultipleAnnotatedMethods() {
		ConfigWithMultipleMethods config = new ConfigWithMultipleMethods();

		PostProcessor.process(config);

		assertEquals(3, config.executionOrder.size());
		assertTrue(config.executionOrder.contains("first"));
		assertTrue(config.executionOrder.contains("second"));
		assertTrue(config.executionOrder.contains("third"));
	}

	@Test
	void processThrowsExceptionForMethodWithParameters() {
		InvalidConfigWithParams config = new InvalidConfigWithParams();

		ConfigException exception = assertThrows(ConfigException.class, () -> {
			PostProcessor.process(config);
		});

		assertTrue(exception.getMessage().contains("no parameters"));
	}

	@Test
	void processThrowsExceptionForMethodWithReturnValue() {
		InvalidConfigWithReturn config = new InvalidConfigWithReturn();

		ConfigException exception = assertThrows(ConfigException.class, () -> {
			PostProcessor.process(config);
		});

		assertTrue(exception.getMessage().contains("return void"));
	}

	@Test
	void processThrowsExceptionForNonPublicMethod() {
		InvalidConfigNotPublic config = new InvalidConfigNotPublic();

		ConfigException exception = assertThrows(ConfigException.class, () -> {
			PostProcessor.process(config);
		});

		assertTrue(exception.getMessage().contains("public"));
	}

	@Test
	void processCallsMethodsFromSuperclass() {
		ConfigWithInheritance config = new ConfigWithInheritance();

		PostProcessor.process(config);

		assertEquals(1, config.baseCallCount);
		assertEquals(1, config.childCallCount);
	}

	@Test
	void processHandlesMultipleConfigsIndependently() {
		SimpleConfig config1 = new SimpleConfig();
		config1.name = "first";

		SimpleConfig config2 = new SimpleConfig();
		config2.name = "second";

		PostProcessor.process(config1);
		PostProcessor.process(config2);

		assertEquals("FIRST", config1.name);
		assertEquals("SECOND", config2.name);
		assertEquals(1, config1.callCount);
		assertEquals(1, config2.callCount);
	}

	static class ConfigThatThrowsException {
		@PostProcess
		public void failingMethod() {
			throw new RuntimeException("Intentional failure");
		}
	}

	@Test
	void processWrapsMethodExceptionsInConfigException() {
		ConfigThatThrowsException config = new ConfigThatThrowsException();

		ConfigException exception = assertThrows(ConfigException.class, () -> {
			PostProcessor.process(config);
		});

		assertTrue(exception.getMessage().contains("Failed to invoke"));
		assertTrue(exception.getMessage().contains("failingMethod"));
		assertNotNull(exception.getCause());
	}
}

