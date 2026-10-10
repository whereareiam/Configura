package me.whereareiam.configura.feature.polymorphic;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.feature.polymorphic.api.annotation.Polymorphic;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What a polymorphic base type is bound to when its document names a subtype, names none, or names
 * one that does not exist.
 */
class PolymorphicResolutionTest {
	@Polymorphic(
			discriminator = "type",
			mappings = {
					@Polymorphic.Type(value = "SQLITE", target = Sqlite.class),
					@Polymorphic.Type(value = "POSTGRES", target = Postgres.class)
			}
	)
	public abstract static class Storage {
		public String type;
	}

	public static class Sqlite extends Storage {
		public String file = "data.db";
	}

	public static class Postgres extends Storage {
		public String host = "localhost";
	}

	@Polymorphic(
			discriminator = "type",
			mappings = {
					@Polymorphic.Type(value = "SQLITE", target = DefaultedSqlite.class),
					@Polymorphic.Type(value = "POSTGRES", target = DefaultedPostgres.class)
			},
			defaultValue = "SQLITE"
	)
	public abstract static class DefaultedStorage {
		public String type;
	}

	public static class DefaultedSqlite extends DefaultedStorage {
		public String file = "data.db";
	}

	public static class DefaultedPostgres extends DefaultedStorage {
		public String host = "localhost";
	}

	@Polymorphic(
			discriminator = "kind",
			mappings = {
					@Polymorphic.Type(value = "PLAIN", target = Shape.class),
					@Polymorphic.Type(value = "CIRCLE", target = Circle.class)
			}
	)
	public static class Shape {
		public String kind;
		public String name = "shape";
	}

	public static class Circle extends Shape {
		public int radius = 1;
	}

	@Polymorphic(
			discriminator = "type",
			mappings = @Polymorphic.Type(value = "KNOWN", target = Known.class),
			defaultTarget = Fallback.class
	)
	public abstract static class WithDefaultTarget {
		public String type;
	}

	public static class Known extends WithDefaultTarget {
	}

	public static class Fallback extends WithDefaultTarget {
	}

	@Polymorphic(inferBy = {
			@Polymorphic.Infer(field = "servers", target = ByServers.class),
			@Polymorphic.Infer(field = "worlds", target = ByWorlds.class)
	})
	public interface Scope {
	}

	public static class ByServers implements Scope {
		public List<String> servers;
	}

	public static class ByWorlds implements Scope {
		public List<String> worlds;
	}

	public static class Holder {
		public List<Storage> storages;
	}

	@Test
	void bindsTheSubtypeItsDiscriminatorNames() {
		Storage storage = read("type: POSTGRES\nhost: db\n", Storage.class);

		assertInstanceOf(Postgres.class, storage);
		assertEquals("db", ((Postgres) storage).host);
	}

	@Test
	void unknownDiscriminatorOnAbstractBaseNamesTheValueAndTheAcceptedOnes() {
		ConfigException failure = assertThrows(ConfigException.class, () -> read("type: ORACLE\n", Storage.class));

		assertEquals(
				"Unknown 'type' value 'ORACLE' for " + Storage.class.getName() + "; accepted values: SQLITE, POSTGRES",
				rootMessage(failure)
		);
	}

	@Test
	void discriminatorIsMatchedExactly() {
		ConfigException failure = assertThrows(ConfigException.class, () -> read("type: sqlite\n", Storage.class));

		assertTrue(rootMessage(failure).startsWith("Unknown 'type' value 'sqlite' for "), rootMessage(failure));
	}

	@Test
	void unknownDiscriminatorInsideADocumentNamesWhereItIs() {
		String yaml = """
				storages:
				  - type: SQLITE
				  - type: ORACLE
				""";

		ConfigException failure = assertThrows(ConfigException.class, () -> read(yaml, Holder.class));

		assertEquals(
				"Unknown 'type' value 'ORACLE' for " + Storage.class.getName() + " at /storages/1; accepted values: SQLITE, POSTGRES",
				configMessage(failure)
		);
	}

	@Test
	void unknownDiscriminatorIsRefusedEvenWhenADefaultValueExists() {
		ConfigException failure = assertThrows(ConfigException.class, () -> read("type: ORACLE\n", DefaultedStorage.class));

		assertTrue(rootMessage(failure).startsWith("Unknown 'type' value 'ORACLE' for "), rootMessage(failure));
	}

	@Test
	void unknownDiscriminatorOnConcreteBaseIsRefusedToo() {
		ConfigException failure = assertThrows(ConfigException.class, () -> read("kind: SQUARE\n", Shape.class));

		assertEquals(
				"Unknown 'kind' value 'SQUARE' for " + Shape.class.getName() + "; accepted values: PLAIN, CIRCLE",
				rootMessage(failure)
		);
	}

	@Test
	void missingDiscriminatorUsesTheDefaultValue() {
		assertInstanceOf(DefaultedSqlite.class, read("file: other.db\n", DefaultedStorage.class));
	}

	@Test
	void nonTextDiscriminatorCountsAsMissing() {
		assertInstanceOf(DefaultedSqlite.class, read("type: 5\n", DefaultedStorage.class));

		ConfigException failure = assertThrows(ConfigException.class, () -> read("type: 5\n", Storage.class));
		assertEquals(
				"Cannot choose a subtype of " + Storage.class.getName() + ": 'type' is missing; accepted values: SQLITE, POSTGRES",
				rootMessage(failure)
		);
	}

	@Test
	void missingDiscriminatorWithoutDefaultOnAbstractBaseIsRefused() {
		ConfigException failure = assertThrows(ConfigException.class, () -> read("file: other.db\n", Storage.class));

		assertEquals(
				"Cannot choose a subtype of " + Storage.class.getName() + ": 'type' is missing; accepted values: SQLITE, POSTGRES",
				rootMessage(failure)
		);
	}

	@Test
	void missingDiscriminatorOnConcreteBaseBindsTheBaseItself() {
		Shape shape = read("name: plain\n", Shape.class);

		assertSame(Shape.class, shape.getClass());
		assertEquals("plain", shape.name);
	}

	@Test
	void discriminatorMappedToTheBaseBindsTheBaseItself() {
		Shape shape = read("kind: PLAIN\nname: plain\n", Shape.class);

		assertSame(Shape.class, shape.getClass());
		assertEquals("PLAIN", shape.kind);
	}

	@Test
	void defaultTargetStillTakesWhatNothingElseResolves() {
		assertInstanceOf(Known.class, read("type: KNOWN\n", WithDefaultTarget.class));
		assertInstanceOf(Fallback.class, read("type: OTHER\n", WithDefaultTarget.class));
		assertInstanceOf(Fallback.class, read("{}\n", WithDefaultTarget.class));
	}

	@Test
	void inferenceWithoutAMatchingFieldIsRefused() {
		assertInstanceOf(ByWorlds.class, read("worlds: [a]\n", Scope.class));

		ConfigException failure = assertThrows(ConfigException.class, () -> read("other: 1\n", Scope.class));
		assertEquals(
				"Cannot choose a subtype of " + Scope.class.getName() + ": it has none of the fields servers, worlds",
				rootMessage(failure)
		);
	}

	@Test
	void concreteBaseIsWrittenAndReadBack() {
		Shape shape = new Shape();
		shape.name = "plain";

		String written = new String(configura().writeBytes(shape), StandardCharsets.UTF_8);
		Shape read = read(written, Shape.class);

		assertSame(Shape.class, read.getClass());
		assertEquals("plain", read.name);
	}

	@Test
	void subtypeIsWrittenWithItsDiscriminatorAndReadBack() {
		Circle circle = new Circle();
		circle.kind = "CIRCLE";
		circle.radius = 3;

		String written = new String(configura().writeBytes(circle), StandardCharsets.UTF_8);
		Shape read = read(written, Shape.class);

		assertInstanceOf(Circle.class, read);
		assertEquals(3, ((Circle) read).radius);
	}

	@Test
	void subtypeInsideADocumentIsWrittenAndReadBack() {
		Holder holder = new Holder();
		Postgres postgres = new Postgres();
		postgres.type = "POSTGRES";
		postgres.host = "db";
		holder.storages = List.of(new Sqlite(), postgres);
		holder.storages.get(0).type = "SQLITE";

		Holder read = read(new String(configura().writeBytes(holder), StandardCharsets.UTF_8), Holder.class);

		assertInstanceOf(Sqlite.class, read.storages.get(0));
		assertEquals("db", ((Postgres) read.storages.get(1)).host);
	}

	private static <T> T read(String yaml, Class<T> type) {
		return configura().read(yaml.getBytes(StandardCharsets.UTF_8), type);
	}

	private static Configura configura() {
		return Config.builder()
				.format(Format.YAML)
				.feature(PolymorphicFeature.defaults())
				.build();
	}

	/** The message of the innermost cause, which is what the feature reported. */
	private static String rootMessage(Throwable failure) {
		Throwable cause = failure;
		while (cause.getCause() != null) cause = cause.getCause();

		return cause.getMessage();
	}

	/** The message of the innermost ConfigException, below Jackson's wrapping of nested failures. */
	private static String configMessage(Throwable failure) {
		String message = null;
		for (Throwable cause = failure; cause != null; cause = cause.getCause())
			if (cause instanceof ConfigException) message = cause.getMessage();

		return message;
	}
}
