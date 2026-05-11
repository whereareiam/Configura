package me.whereareiam.configura;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.configura.merge.MergeDefaultsProvider;
import me.whereareiam.configura.merge.strategy.DeclaredKeysOnlyMap;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
	static class SampleConfig {
		public String value;
	}

	public static class SampleDefaults implements MergeDefaultsProvider<SampleConfig> {
		@Override
		public SampleConfig supply(SampleConfig config) {
			config.value = "sample";
			return config;
		}
	}

	@Test
	void builderUsesRequestedFormat() {
		Config config = Config.builder()
				.format(Format.JSON)
				.build();

		assertEquals(".json", config.extension());
	}

	@Test
	void withDefaultsRegistersDefaultsProvider() {
		Config config = Config.yaml().withDefaults(SampleDefaults.class);

        assertInstanceOf(SampleDefaults.class, config.registeredDefaults().getDefaultsProvider(SampleConfig.class));
	}

	@Test
	void withMergeStrategyRegistersNamedStrategy() {
		Config config = Config.yaml().withMergeStrategy("declaredKeysOnly", DeclaredKeysOnlyMap.class);

		assertEquals(DeclaredKeysOnlyMap.class, config.mergeStrategies().get("declaredKeysOnly"));
	}

	@Test
	void withModuleReturnsConfigIncludingModule() {
		Module module = new SimpleModule("test");
		Config config = Config.yaml().withModule(module);

		assertTrue(config.modules().contains(module));
	}
}
