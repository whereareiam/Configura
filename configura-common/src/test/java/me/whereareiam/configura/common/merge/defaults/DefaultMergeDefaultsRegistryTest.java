package me.whereareiam.configura.common.merge.defaults;

import me.whereareiam.configura.merge.MergeDefaultsProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultMergeDefaultsRegistryTest {
	static class Root {
		public String name;
	}

	public static class RootProvider implements MergeDefaultsProvider<Root> {
		@Override
		public Root supply(Root instance) {
			instance.name = "x";
			return instance;
		}
	}

	@Test
	void registerAndFetchInstance() {
		DefaultMergeDefaultsRegistry registry = new DefaultMergeDefaultsRegistry();
		registry.registerDefaults(RootProvider.class);

		MergeDefaultsProvider<Root> provider = registry.getDefaultsProvider(Root.class);
		assertNotNull(provider);

		Root root = provider.supply(new Root());
		assertEquals("x", root.name);
	}
}
