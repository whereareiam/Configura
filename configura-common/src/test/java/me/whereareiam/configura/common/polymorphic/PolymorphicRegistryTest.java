package me.whereareiam.configura.common.polymorphic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PolymorphicRegistryTest {
	static class BaseType {
	}

	static class AlphaType extends BaseType {
	}

	static class BetaType extends BaseType {
	}

	@Test
	void registerStoresDefinition() {
		PolymorphicRegistry.register(BaseType.class)
				.discriminator("type")
				.map("ALPHA", AlphaType.class)
				.defaultValue("ALPHA")
				.defaultTarget(AlphaType.class)
				.build();

		PolymorphicDefinition definition = PolymorphicRegistry.get(BaseType.class);
		assertEquals("type", definition.getDiscriminator());
		assertEquals(AlphaType.class, definition.getMappings().get("ALPHA"));
		assertEquals("ALPHA", definition.getDefaultValue());
		assertEquals(AlphaType.class, definition.getDefaultTarget());
	}

	@Test
	void registerMergesExistingDefinition() {
		PolymorphicRegistry.register(BaseType.class)
				.inferByField("alpha", AlphaType.class)
				.build();

		PolymorphicRegistry.register(BaseType.class)
				.inferByField("beta", BetaType.class)
				.build();

		PolymorphicDefinition definition = PolymorphicRegistry.get(BaseType.class);
		assertEquals(AlphaType.class, definition.getInferFields().get("alpha"));
		assertEquals(BetaType.class, definition.getInferFields().get("beta"));
		assertNull(definition.getMappings().get("BETA"));
	}
}
