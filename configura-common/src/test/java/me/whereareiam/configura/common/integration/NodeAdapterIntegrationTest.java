package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.node.NumberNode;
import me.whereareiam.configura.node.ObjectNode;
import me.whereareiam.configura.node.StringNode;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeAdapterIntegrationTest {
	static class Color {
		int red;
		int green;
		int blue;
	}

	public static class ColorAdapter implements TypeAdapter<Color> {
		@Override
		public Color deserialize(String value) {
			throw new IllegalStateException("String deserialization is not expected");
		}

		@Override
		public String serialize(Color value) {
			throw new IllegalStateException("String serialization is not expected");
		}

		@Override
		public Color deserializeNode(Node node) {
			if (!(node instanceof ObjectNode objectNode)) {
				throw new IllegalArgumentException("Color must be an object");
			}

			Map<String, Node> values = objectNode.getValues();
			Color color = new Color();
			color.red = readInt(values.get("r"), "r");
			color.green = readInt(values.get("g"), "g");
			color.blue = readInt(values.get("b"), "b");
			return color;
		}

		@Override
		public Node serializeNode(Color value) {
			Map<String, Node> values = new LinkedHashMap<>();
			values.put("r", new NumberNode(value.red));
			values.put("g", new NumberNode(value.green));
			values.put("b", new NumberNode(value.blue));
			return new ObjectNode(values);
		}

		private int readInt(Node node, String key) {
			if (node instanceof NumberNode numberNode && numberNode.getValue() != null) {
				return numberNode.getValue().intValue();
			}
			if (node instanceof StringNode stringNode) {
				return Integer.parseInt(stringNode.getValue());
			}
			throw new IllegalArgumentException("Missing or invalid '" + key + "' value");
		}
	}

	static class Palette {
		@Field
		public Color primary;
	}

	@Test
	void adapterRoundtripWithNode(@TempDir Path dir) throws IOException {
		ConfigReader reader = new DefaultConfigReader().withFormat(Format.YAML)
				.registerAdapter(Color.class, ColorAdapter.class);
		ConfigWriter writer = new DefaultConfigWriter().withFormat(Format.YAML)
				.registerAdapter(Color.class, ColorAdapter.class);

		Palette palette = new Palette();
		palette.primary = new Color();
		palette.primary.red = 12;
		palette.primary.green = 34;
		palette.primary.blue = 56;

		Path file = dir.resolve("palette.yml");
		writer.encode(file, palette);

		String content = Files.readString(file);
		assertTrue(content.contains("r:"), "Expected object field 'r' in output");
		assertTrue(content.contains("g:"), "Expected object field 'g' in output");
		assertTrue(content.contains("b:"), "Expected object field 'b' in output");

		Palette read = reader.load(file, Palette.class);
		assertEquals(12, read.primary.red);
		assertEquals(34, read.primary.green);
		assertEquals(56, read.primary.blue);
	}
}
