package me.whereareiam.configura.feature.polymorphic;


import me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultPolymorphicRegistryTest {
	static class BaseType {
	}

	static class AlphaType extends BaseType {
	}

	static class BetaType extends BaseType {
	}

	@Test
	void registerStoresDefinition() {
		PolymorphicFeature feature = PolymorphicFeature.defaults();
		feature.register(BaseType.class)
				.discriminator("type")
				.map("ALPHA", AlphaType.class)
				.defaultValue("ALPHA")
				.defaultTarget(AlphaType.class)
				.build();

		PolymorphicDefinition definition = (PolymorphicDefinition) feature.definition(BaseType.class);
		assertEquals("type", definition.getDiscriminator());
		assertEquals(AlphaType.class, definition.getMappings().get("ALPHA"));
		assertEquals("ALPHA", definition.getDefaultValue());
		assertEquals(AlphaType.class, definition.getDefaultTarget());
	}

	@Test
	void registerOverwritesDefinition() {
		PolymorphicFeature feature = PolymorphicFeature.defaults();
		feature.register(BaseType.class)
				.inferByField("alpha", AlphaType.class)
				.build();

		feature.register(BaseType.class)
				.inferByField("beta", BetaType.class)
				.build();

		PolymorphicDefinition definition = (PolymorphicDefinition) feature.definition(BaseType.class);
		assertEquals(BetaType.class, definition.getInferFields().get("beta"));
	}
}
