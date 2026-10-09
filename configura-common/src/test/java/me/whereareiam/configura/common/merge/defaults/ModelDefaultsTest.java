package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.document.DefaultDocumentProcessor;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelDefaultsTest {
	private final ObjectMapper mapper = MapperFactory.createYamlMapper(List.of());
	private final DefaultsProviderRegistry providers = new DefaultsProviderRegistry();
	private final ModelDefaults defaults = new ModelDefaults(mapper, providers, new DefaultDocumentProcessor(List.of(), List.of()));

	@Test
	void takesTheValuesOfAConstructedModel() {
		ObjectNode node = defaults.of(new Settings(), Settings.class, PrimitiveDefaultPolicy.AS_MISSING);

		assertEquals("lobby", node.path("name").asText());
		assertFalse(node.has("motd"));
	}

	@Test
	void givesANullSectionTheDefaultsOfItsType() {
		ObjectNode node = defaults.of(new Settings(), Settings.class, PrimitiveDefaultPolicy.AS_MISSING);

		assertEquals(3, node.at("/retry/attempts").asInt());
		assertFalse(node.at("/retry/enabled").asBoolean());
	}

	@Test
	void letsARootProviderFillWhatInitializersLeftUnset() {
		providers.registerProvider(SettingsDefaults.class);

		ObjectNode node = defaults.of(new Settings(), Settings.class, PrimitiveDefaultPolicy.AS_MISSING);

		assertEquals("lobby", node.path("name").asText());
		assertEquals("welcome", node.path("motd").asText());
		assertEquals(25565, node.path("port").asInt());
	}

	@Test
	void keepsTheZerosOfAModelTheCallerFilledIn() {
		providers.registerProvider(SettingsDefaults.class);

		ObjectNode node = defaults.of(new Settings(), Settings.class, PrimitiveDefaultPolicy.PRESERVE);

		assertEquals(0, node.path("port").asInt());
		assertEquals("welcome", node.path("motd").asText());
	}

	@Test
	void takesWhatASectionsProviderReturnsAsItsDefault() {
		providers.registerProvider(RetryDefaults.class);

		ObjectNode node = defaults.of(new Settings(), Settings.class, PrimitiveDefaultPolicy.PRESERVE);

		assertEquals(5, node.at("/retry/attempts").asInt());
		assertTrue(node.at("/retry/enabled").asBoolean());
	}

	@Test
	void runsTheProvidersOfSuperclassesFirst() {
		providers.registerProvider(RetryDefaults.class);
		providers.registerProvider(BackoffDefaults.class);

		ObjectNode node = defaults.ofType(Backoff.class, PrimitiveDefaultPolicy.AS_MISSING, null);

		assertEquals(7, node.path("attempts").asInt());
		assertTrue(node.path("enabled").asBoolean());
	}

	@Test
	void hasNoDefaultsForTypesThatAreNotModels() {
		assertNull(defaults.ofType(String.class, PrimitiveDefaultPolicy.AS_MISSING, null));
		assertNull(defaults.ofType(List.class, PrimitiveDefaultPolicy.AS_MISSING, null));
	}

	public static class Settings {
		public String name = "lobby";
		public String motd;
		public int port;
		public Retry retry;
	}

	public static class Retry {
		public int attempts = 3;
		public boolean enabled;
	}

	public static class Backoff extends Retry {
	}

	public static class SettingsDefaults implements DefaultsProvider<Settings> {
		@Override
		public Settings supply(Settings settings) {
			settings.name = "ignored at the root";
			settings.motd = "welcome";
			settings.port = 25565;
			return settings;
		}
	}

	public static class RetryDefaults implements DefaultsProvider<Retry> {
		@Override
		public Retry supply(Retry retry) {
			retry.attempts = 5;
			retry.enabled = true;
			return retry;
		}
	}

	public static class BackoffDefaults implements DefaultsProvider<Backoff> {
		@Override
		public Backoff supply(Backoff backoff) {
			backoff.attempts = 7;
			return backoff;
		}
	}
}
