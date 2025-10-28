package me.whereareiam.configura.common.template;

import me.whereareiam.configura.TemplateProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TemplateRegistryTest {
	static class Root {
		public String name;
	}

	public static class RootProvider implements TemplateProvider<Root> {
		@Override
		public Root supply(Class<Root> targetType) {
			Root r = new Root();
			r.name = "x";
			return r;
		}
	}

	@Test
	void registerAndFetchInstance() {
		TemplateRegistry.registerModel(Root.class, new RootProvider());

		TemplateProvider<Root> provider = TemplateRegistry.getModelProvider(Root.class);
		assertNotNull(provider);

		Root root = provider.supply(Root.class);
		assertEquals("x", root.name);
	}
}


