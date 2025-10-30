package me.whereareiam.configura.common.template;

import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.template.TemplateRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TemplateRegistryTest {
	static class Root {
		public String name;
	}

	public static class RootProvider implements TemplateProvider<Root> {
		@Override
		public Root supply(Root instance) {
			instance.name = "x";
			return instance;
		}
	}

	@Test
	void registerAndFetchInstance() {
		TemplateRegistry registry = new DefaultTemplateRegistry();
		registry.registerTemplate(RootProvider.class);

		TemplateProvider<Root> provider = registry.getTemplateProvider(Root.class);
		assertNotNull(provider);

		Root root = provider.supply(new Root());
		assertEquals("x", root.name);
	}
}


