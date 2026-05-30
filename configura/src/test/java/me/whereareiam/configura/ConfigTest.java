package me.whereareiam.configura;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.policy.MergePolicyResolver;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.strategy.StructuralObject;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.MergeTypeAdapterRegistry;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import me.whereareiam.configura.type.Format;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
	static class SampleConfig {
		public String value;
	}

	public static class SampleDefaults implements DefaultsProvider<SampleConfig> {
		@Override
		public SampleConfig supply(SampleConfig config) {
			config.value = "sample";
			return config;
		}
	}

	private static final MergeTypeAdapter TEST_ADAPTER = new MergeTypeAdapter() {
		@Override
		public boolean supports(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
			return false;
		}

		@Override
		public @NotNull Class<?> resolveChildType(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
			return descriptor.getDeclaredType();
		}

		@Override
		public com.fasterxml.jackson.databind.@NotNull JsonNode merge(@NotNull MergeTypeAdapterContext context) {
			return context.getMapper().nullNode();
		}
	};

	private static final MergeTypeAdapter SECOND_ADAPTER = new MergeTypeAdapter() {
		@Override
		public boolean supports(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
			return false;
		}

		@Override
		public @NotNull Class<?> resolveChildType(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
			return descriptor.getDeclaredType();
		}

		@Override
		public com.fasterxml.jackson.databind.@NotNull JsonNode merge(@NotNull MergeTypeAdapterContext context) {
			return context.getMapper().nullNode();
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
	void builderRegistersBuiltInTypeAdaptersAndPolicyResolvers() {
		Configura config = Config.builder().build();

		assertFalse(config.typeAdapters().isEmpty());
		assertFalse(config.policyResolvers().isEmpty());
	}

	@Test
	void withTypeAdapterReturnsIndependentConfig() {
		Configura base = Config.yaml();
		Configura configured = base.withTypeAdapter(TEST_ADAPTER);

		assertFalse(base.typeAdapters().contains(TEST_ADAPTER));
		assertTrue(configured.typeAdapters().contains(TEST_ADAPTER));
	}

	@Test
	void withPolicyResolverReturnsIndependentConfig() {
		Configura base = Config.yaml();
		Configura configured = base.withPolicyResolver(TEST_RESOLVER);

		assertFalse(base.policyResolvers().contains(TEST_RESOLVER));
		assertTrue(configured.policyResolvers().contains(TEST_RESOLVER));
	}

	@Test
	void mergeTypeAdapterRegistryCopyIsIndependent() {
		MergeTypeAdapterRegistry registry = new MergeTypeAdapterRegistry().register(TEST_ADAPTER);
		MergeTypeAdapterRegistry copy = registry.copy().register(SECOND_ADAPTER);

		assertEquals(1, registry.asList().size());
		assertEquals(2, copy.asList().size());
		assertFalse(registry.asList().contains(SECOND_ADAPTER));
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
		Configura original = Config.configured();
		try {
			Config.configure(builder -> builder.format(Format.JSON));

			assertEquals(".yml", Config.configured().extension());
			assertEquals(".json", Config.json().extension());
		} finally {
			Config.setConfigured(original);
		}
	}

	@Test
	void builtInstancesStayIndependentFromLaterDefaultChanges() {
		Configura original = Config.configured();
		try {
			Configura built = Config.builder().format(Format.YAML).build();

			Config.configure(builder -> builder.format(Format.JSON));

			assertEquals(".yml", built.extension());
			assertEquals(".yml", Config.configured().extension());
			SampleConfig sample = new SampleConfig();
			sample.value = "sample";
			assertTrue(new String(Config.writeBytes(sample)).trim().startsWith("{"));
		} finally {
			Config.setConfigured(original);
		}
	}
}
