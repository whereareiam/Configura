package me.whereareiam.configura.feature.comment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.comment.api.annotation.Comment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommentIntegrationTest {
	private static final Configura YAML = Configura.builder()
			.feature(CommentFeature.defaults())
			.build();
	private static final Configura JSON = YAML.toBuilder()
			.format(Format.JSON)
			.build();

	@TempDir
	Path directory;

	@Test
	void yamlWritesCommentsAboveTheirKeys() throws Exception {
		Path file = directory.resolve("settings.yml");

		YAML.update(file, Settings.class);

		assertEquals("""
				# Settings of the lobby.

				# Name shown to players.
				NAME: "lobby"
				# Retry policy.
				retry:
				  # How often to retry.
				  #
				  # 0 disables retries.
				  attempts: 3
				servers:
				  - # Host of the server.
				    host: "hub"
				routes:
				  main:
				    # Host of the server.
				    host: "arena"
				""", Files.readString(file));
	}

	@Test
	void yamlCommentsSurviveARewriteAndAreReadBack() throws Exception {
		Path file = directory.resolve("settings.yml");
		Files.writeString(file, "NAME: hub\n");

		YAML.update(file, Settings.class);
		Settings settings = YAML.update(file, Settings.class);

		assertEquals("hub", settings.name);
		assertEquals(3, settings.retry.attempts);
		assertEquals(1, Files.readString(file).split("# Name shown to players.", -1).length - 1);
	}

	@Test
	void jsonWritesLineCommentsThatAreReadBack() throws Exception {
		Path file = directory.resolve("settings.json");
		JSON.update(file, Settings.class);
		String written = Files.readString(file);
		Settings settings = JSON.read(file, Settings.class);

		assertEquals("""
				// Settings of the lobby.

				{
				  // Name shown to players.
				  "NAME": "lobby",
				  // Retry policy.
				  "retry": {
				    // How often to retry.
				    //
				    // 0 disables retries.
				    "attempts": 3
				  },
				  "servers": [
				    {
				      // Host of the server.
				      "host": "hub"
				    }
				  ],
				  "routes": {
				    "main": {
				      // Host of the server.
				      "host": "arena"
				    }
				  }
				}""", written);
		assertEquals("lobby", settings.name);
	}

	@Test
	void writeBytesCarriesComments() {
		String written = new String(YAML.writeBytes(new Settings()));

		assertEquals("# Settings of the lobby.", written.lines().findFirst().orElseThrow());
	}

	@Test
	void writeNodeHasNoModelAndWritesNoComments() throws Exception {
		Path file = directory.resolve("settings.yml");
		YAML.writeNode(file, YAML.mapper().valueToTree(new Settings()));

		assertFalse(Files.readString(file).contains("#"));
	}

	@Test
	void formatsWithoutLinesWriteNoComments() throws Exception {
		Path file = directory.resolve("settings.json");
		Configura configura = Configura.builder()
				.format("json", modules -> new ObjectMapper().registerModules(modules))
				.feature(CommentFeature.defaults())
				.build();

		configura.update(file, Settings.class);

		assertFalse(Files.readString(file).contains("/"));
	}

	@Test
	void withoutTheFeatureCommentsAreIgnored() throws Exception {
		Path file = directory.resolve("settings.yml");

		Config.yaml().update(file, Settings.class);

		assertFalse(Files.readString(file).contains("#"));
	}

	@Test
	void documentsWithoutCommentsAreWrittenAsWithoutTheFeature() {
		Plain plain = new Plain();

		assertEquals(new String(Config.yaml().writeBytes(plain)), new String(YAML.writeBytes(plain)));
		assertEquals(new String(Config.json().writeBytes(plain)), new String(JSON.writeBytes(plain)));
	}

	public static class Plain {
		public String name = "lobby";
		public List<String> servers = List.of("hub", "arena");
		public Map<String, Integer> slots = new LinkedHashMap<>(Map.of("main", 1));
	}

	@Comment("Settings of the lobby.")
	@JsonPropertyOrder({"NAME", "retry", "servers", "routes"})
	public static class Settings {
		@Comment("Name shown to players.")
		@JsonProperty("NAME")
		public String name = "lobby";

		@Comment("Retry policy.")
		public Retry retry = new Retry();

		public List<Server> servers = List.of(new Server("hub"));

		public Map<String, Server> routes = new LinkedHashMap<>(Map.of("main", new Server("arena")));
	}

	public static class Retry {
		@Comment({"How often to retry.", "", "0 disables retries."})
		public int attempts = 3;
	}

	public static class Server {
		@Comment("Host of the server.")
		public String host;

		public Server() {
		}

		Server(String host) {
			this.host = host;
		}
	}
}
