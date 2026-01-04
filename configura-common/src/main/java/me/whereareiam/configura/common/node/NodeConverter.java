package me.whereareiam.configura.common.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import me.whereareiam.configura.node.ArrayNode;
import me.whereareiam.configura.node.BooleanNode;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.node.NullNode;
import me.whereareiam.configura.node.NumberNode;
import me.whereareiam.configura.node.ObjectNode;
import me.whereareiam.configura.node.StringNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NodeConverter {
	private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

	public static Node fromJsonNode(JsonNode node) {
		if (node == null || node.isNull()) return NullNode.instance();
		if (node.isTextual()) return new StringNode(node.textValue());
		if (node.isNumber()) return new NumberNode(node.numberValue());
		if (node.isBoolean()) return new BooleanNode(node.booleanValue());
		if (node.isArray()) {
			List<Node> values = new ArrayList<>();
			for (JsonNode element : node) {
				values.add(fromJsonNode(element));
			}
			return new ArrayNode(values);
		}
		if (node.isObject()) {
			Map<String, Node> values = new LinkedHashMap<>();
			for (Map.Entry<String, JsonNode> entry : node.properties()) {
				values.put(entry.getKey(), fromJsonNode(entry.getValue()));
			}
			return new ObjectNode(values);
		}

		return new StringNode(node.asText());
	}

	public static JsonNode toJsonNode(Node node) {
		if (node == null || node instanceof NullNode) return FACTORY.nullNode();
		if (node instanceof StringNode stringNode) return FACTORY.textNode(stringNode.getValue());
		if (node instanceof BooleanNode booleanNode) return FACTORY.booleanNode(booleanNode.getValue());
		if (node instanceof NumberNode numberNode) return toNumberNode(numberNode.getValue());
		if (node instanceof ArrayNode arrayNode) {
			com.fasterxml.jackson.databind.node.ArrayNode array = FACTORY.arrayNode();
			for (Node value : arrayNode.getValues()) {
				array.add(toJsonNode(value));
			}
			return array;
		}
		if (node instanceof ObjectNode objectNode) {
			com.fasterxml.jackson.databind.node.ObjectNode object = FACTORY.objectNode();
			for (Map.Entry<String, Node> entry : objectNode.getValues().entrySet()) {
				object.set(entry.getKey(), toJsonNode(entry.getValue()));
			}
			return object;
		}

		return FACTORY.textNode(node.asText());
	}

	private static JsonNode toNumberNode(Number number) {
		if (number == null) return FACTORY.nullNode();
		if (number instanceof Integer value) return FACTORY.numberNode(value);
		if (number instanceof Long value) return FACTORY.numberNode(value);
		if (number instanceof Short value) return FACTORY.numberNode(value);
		if (number instanceof Byte value) return FACTORY.numberNode(value);
		if (number instanceof Double value) return FACTORY.numberNode(value);
		if (number instanceof Float value) return FACTORY.numberNode(value);
		if (number instanceof BigInteger value) return FACTORY.numberNode(value);
		if (number instanceof BigDecimal value) return FACTORY.numberNode(value);
		return FACTORY.numberNode(new BigDecimal(number.toString()));
	}
}
