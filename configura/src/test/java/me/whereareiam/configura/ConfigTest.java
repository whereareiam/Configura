package me.whereareiam.configura;

import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigTest {
	@Test
	void sharesTheInstanceItWasGiven() {
		Configura original = Config.configured();
		Configura json = Config.json();
		try {
			Config.setConfigured(json);

			assertSame(json, Config.configured());
		} finally {
			Config.setConfigured(original);
		}
	}

	@Test
	void buildsInstancesTheSharedOneHasNoInfluenceOn() {
		Configura original = Config.configured();
		try {
			Config.setConfigured(Config.json());

			assertEquals(".yml", Config.builder().build().extension());
			assertEquals(".yml", Config.yaml().extension());
			assertEquals(".json", Config.builder().format(Format.JSON).build().extension());
		} finally {
			Config.setConfigured(original);
		}
	}
}
