package me.whereareiam.configura.node;

@SuppressWarnings("unused")
public interface Node {
	NodeType type();

	String asText();

	default boolean isObject() {
		return type() == NodeType.OBJECT;
	}

	default boolean isArray() {
		return type() == NodeType.ARRAY;
	}

	default boolean isString() {
		return type() == NodeType.STRING;
	}

	default boolean isNumber() {
		return type() == NodeType.NUMBER;
	}

	default boolean isBoolean() {
		return type() == NodeType.BOOLEAN;
	}

	default boolean isNull() {
		return type() == NodeType.NULL;
	}
}
