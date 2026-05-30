package me.whereareiam.configura.common.merge.defaults;

import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultsProviderRegistryTest {
	static class Root {
		public String name;
	}

	public static class RootProvider implements DefaultsProvider<Root> {
		@Override
		public Root supply(Root instance) {
			instance.name = "x";
			return instance;
		}
	}

	@Test
	void registerAndFetchInstance() {
		DefaultsProviderRegistry registry = new DefaultsProviderRegistry();
		registry.registerProvider(RootProvider.class);

		DefaultsProvider<Root> provider = registry.getProvider(Root.class);
		assertNotNull(provider);

		Root root = provider.supply(new Root());
		assertEquals("x", root.name);
	}
}
