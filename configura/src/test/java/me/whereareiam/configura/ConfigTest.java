package me.whereareiam.configura;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.NullNode;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.configura.merge.plugin.MergePlugin;
import me.whereareiam.configura.merge.plugin.MergePluginRegistry;
import me.whereareiam.configura.merge.plugin.context.MergePluginContext;
import me.whereareiam.configura.merge.plugin.context.MergePluginDefaultsContext;
import me.whereareiam.configura.merge.plugin.descriptor.MergeDescriptor;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.policy.MergePolicyResolver;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.strategy.StructuralObject;
import me.whereareiam.configura.type.Format;
import org.jetbrains.annotations.NotNull;
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

	private static final MergePlugin TEST_PLUGIN = new MergePlugin() {
		@Override
		public boolean supports(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
			return false;
		}

		@Override
		public @NotNull Class<?> resolveChildType(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
			return descriptor.getDeclaredType();
		}

		@Override
		public com.fasterxml.jackson.databind.JsonNode resolveDefaultValue(@NotNull MergePluginDefaultsContext context) {
			return NullNode.instance;
		}

		@Override
		public com.fasterxml.jackson.databind.@NotNull JsonNode merge(@NotNull MergePluginContext context) {
			return NullNode.instance;
		}
	};

	private static final MergePlugin SECOND_PLUGIN = new MergePlugin() {
		@Override
		public boolean supports(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
			return false;
		}

		@Override
		public @NotNull Class<?> resolveChildType(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
			return descriptor.getDeclaredType();
		}

		@Override
		public com.fasterxml.jackson.databind.JsonNode resolveDefaultValue(@NotNull MergePluginDefaultsContext context) {
			return NullNode.instance;
		}

		@Override
		public com.fasterxml.jackson.databind.@NotNull JsonNode merge(@NotNull MergePluginContext context) {
			return NullNode.instance;
		}
	};

	private static final MergePolicyResolver TEST_RESOLVER =
			descriptor -> MergePolicy.builder().behaviorOverride(MergeBehavior.defaults()).build();

	private static final MergePolicyResolver SECOND_RESOLVER =
			descriptor -> MergePolicy.builder().namedStrategy("custom").build();

	@Test
	void builderUsesRequestedFormat() {
		Configura config = Config.builder()
				.format(Format.JSON)
				.build();

		assertEquals(".json", config.extension());
	}

	@Test
	void withDefaultsRegistersDefaultsProvider() {
		Configura config = Config.yaml().withDefaults(SampleDefaults.class);

		assertInstanceOf(SampleDefaults.class, config.registeredDefaultProviders().getProvider(SampleConfig.class));
	}

	@Test
	void withStrategyRegistersNamedStrategy() {
		Configura config = Config.yaml().withStrategy("structural", StructuralObject.class);

		assertEquals(StructuralObject.class, config.mergeStrategies().get("structural"));
	}

	@Test
	void builderRegistersBuiltInFieldPluginsAndPolicyResolvers() {
		Configura config = Config.builder().build();

		assertFalse(config.mergePlugins().isEmpty());
		assertFalse(config.policyResolvers().isEmpty());
	}

	@Test
	void withPluginReturnsIndependentConfig() {
		Configura base = Config.yaml();
		Configura configured = base.withPlugin(TEST_PLUGIN);

		assertFalse(base.mergePlugins().contains(TEST_PLUGIN));
		assertTrue(configured.mergePlugins().contains(TEST_PLUGIN));
	}

	@Test
	void withPolicyResolverReturnsIndependentConfig() {
		Configura base = Config.yaml();
		Configura configured = base.withPolicyResolver(TEST_RESOLVER);

		assertFalse(base.policyResolvers().contains(TEST_RESOLVER));
		assertTrue(configured.policyResolvers().contains(TEST_RESOLVER));
	}

	@Test
	void mergePluginRegistryCopyIsIndependent() {
		MergePluginRegistry registry = new MergePluginRegistry().register(TEST_PLUGIN);
		MergePluginRegistry copy = registry.copy().register(SECOND_PLUGIN);

		assertEquals(1, registry.asList().size());
		assertEquals(2, copy.asList().size());
		assertFalse(registry.asList().contains(SECOND_PLUGIN));
	}

	@Test
	void mergePropertyPolicyResolverRegistryCopyIsIndependent() {
		MergePolicyResolverRegistry registry = new MergePolicyResolverRegistry().register(TEST_RESOLVER);
		MergePolicyResolverRegistry copy = registry.copy().register(SECOND_RESOLVER);

		assertEquals(1, registry.asList().size());
		assertEquals(2, copy.asList().size());
		assertFalse(registry.asList().contains(SECOND_RESOLVER));
	}

	@Test
	void withModuleReturnsConfigIncludingModule() {
		Module module = new SimpleModule("test");
		Configura config = Config.yaml().withModule(module);

		assertTrue(config.modules().contains(module));
	}

	@Test
	void configureReplacesDefaultHelper() {
		Configura original = Config.defaults();
		try {
			Config.configure(builder -> builder.format(Format.JSON));

			assertEquals(".yml", Config.defaults().extension());
			assertEquals(".json", Config.json().extension());
		} finally {
			Config.setDefaults(original);
		}
	}

	@Test
	void builtInstancesStayIndependentFromLaterDefaultChanges() {
		Configura original = Config.defaults();
		try {
			Configura built = Config.builder().format(Format.YAML).build();

			Config.configure(builder -> builder.format(Format.JSON));

			assertEquals(".yml", built.extension());
			assertEquals(".yml", Config.defaults().extension());
			SampleConfig sample = new SampleConfig();
			sample.value = "sample";
			assertTrue(new String(Config.writeBytes(sample)).trim().startsWith("{"));
		} finally {
			Config.setDefaults(original);
		}
	}
}
