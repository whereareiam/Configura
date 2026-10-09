package me.whereareiam.configura.merge;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.type.merge.MissingEntries;
import me.whereareiam.configura.type.merge.UnknownEntries;
import me.whereareiam.configura.type.merge.WhenAbsent;
import me.whereareiam.configura.type.merge.WhenPresent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeRulesIntegrationTest {
	private final Configura configura = Config.yaml();

	@TempDir
	Path directory;

	@Test
	void writesEveryDefaultIntoANewFile() {
		Sections sections = update("", Sections.class);

		assertEquals("localhost", sections.filled.host);
		assertEquals("localhost", written().at("/filled/host").asText());
		assertEquals(8080, written().at("/filled/port").asInt());
		assertEquals("localhost", written().at("/kept/host").asText());
	}

	@Test
	void fillsWhatTheUserLeftOutOfASection() {
		Sections sections = update("""
				filled:
				  host: example.com
				""", Sections.class);

		assertEquals("example.com", sections.filled.host);
		assertEquals("example.com", written().at("/filled/host").asText());
		assertEquals(8080, written().at("/filled/port").asInt());
	}

	@Test
	void keepsASectionAsWrittenWhenToldTo() {
		update("""
				kept:
				  host: example.com
				empty: {}
				""", Sections.class);

		assertEquals("example.com", written().at("/kept/host").asText());
		assertFalse(written().path("kept").has("port"));
		assertTrue(written().path("empty").isEmpty());
	}

	@Test
	void leavesAnOptionalSectionOutUntilTheUserAddsIt() {
		Sections absent = update("", Sections.class);

		assertNull(absent.optional);
		assertFalse(written().has("optional"));

		Sections present = update("""
				optional:
				  host: example.com
				""", Sections.class);

		assertEquals("example.com", present.optional.host);
		assertEquals(8080, written().at("/optional/port").asInt());
	}

	@Test
	void neverWritesAFieldThatIsLeftAbsentAndKeptAsWritten() {
		update("", Sections.class);
		assertFalse(written().has("untouched"));

		update("untouched: {}\n", Sections.class);
		assertTrue(written().path("untouched").isEmpty());
	}

	@Test
	void respectsEmptyAndZeroValuesOfTheUser() {
		Values values = update("""
				names: []
				limit: 0
				enabled: false
				aliases: {}
				""", Values.class);

		assertTrue(values.names.isEmpty());
		assertEquals(0, values.limit);
		assertFalse(values.enabled);
		assertTrue(written().path("names").isEmpty());
		assertEquals(0, written().path("limit").asInt());
		assertFalse(written().path("enabled").asBoolean());
	}

	@Test
	void keepsAnExplicitNull() {
		update("names: null\n", Values.class);

		assertTrue(written().path("names").isNull());
	}

	@Test
	void addsDefaultEntriesToAMapUnlessToldToOmitThem() {
		update("""
				seeded:
				  custom: 1
				declared:
				  custom: 1
				""", Maps.class);

		assertEquals(List.of("custom", "first", "second"), keys(written().path("seeded")));
		assertEquals(List.of("custom"), keys(written().path("declared")));
	}

	@Test
	void usesTheWholeDefaultMapWhenTheUserHasNone() {
		update("", Maps.class);

		assertEquals(List.of("first", "second"), keys(written().path("seeded")));
		assertEquals(List.of("first", "second"), keys(written().path("declared")));
	}

	@Test
	void rejectsMapEntriesTheDefaultsDoNotKnowWhenToldTo() {
		RuntimeException failure = assertThrows(RuntimeException.class, () -> update("""
				closed:
				  custom: 1
				""", Maps.class));

		assertTrue(failure.getMessage().contains("custom"), failure.getMessage());
	}

	@Test
	void takesAListWithoutKeyAsAWhole() {
		Values values = update("names: [only]\n", Values.class);

		assertEquals(List.of("only"), values.names);
	}

	private <T> T update(String content, Class<T> type) {
		try {
			Path file = directory.resolve("config.yml");
			Files.writeString(file, content);

			return configura.update(file, type);
		} catch (java.io.IOException failure) {
			throw new java.io.UncheckedIOException(failure);
		}
	}

	private JsonNode written() {
		return configura.readNode(directory.resolve("config.yml"));
	}

	private static List<String> keys(JsonNode node) {
		List<String> keys = new ArrayList<>();
		node.fieldNames().forEachRemaining(keys::add);

		return keys;
	}

	public static class Endpoint {
		public String host = "localhost";
		public int port = 8080;
	}

	public static class Sections {
		public Endpoint filled;

		@Merge(present = WhenPresent.KEEP_AS_WRITTEN)
		public Endpoint kept;

		@Merge(present = WhenPresent.KEEP_AS_WRITTEN)
		public Endpoint empty;

		@Merge(absent = WhenAbsent.LEAVE_ABSENT)
		public Endpoint optional;

		@Merge(absent = WhenAbsent.LEAVE_ABSENT, present = WhenPresent.KEEP_AS_WRITTEN)
		public Endpoint untouched;
	}

	public static class Values {
		public List<String> names = new ArrayList<>(List.of("first", "second"));
		public int limit = 10;
		public boolean enabled = true;
		public Map<String, String> aliases = new LinkedHashMap<>(Map.of("a", "b"));
	}

	public static class Maps {
		public Map<String, Integer> seeded = defaults();

		@Merge(missingEntries = MissingEntries.OMIT)
		public Map<String, Integer> declared = defaults();

		@Merge(unknownEntries = UnknownEntries.REJECT)
		public Map<String, Integer> closed = defaults();

		private static Map<String, Integer> defaults() {
			Map<String, Integer> defaults = new LinkedHashMap<>();
			defaults.put("first", 1);
			defaults.put("second", 2);

			return defaults;
		}
	}
}
