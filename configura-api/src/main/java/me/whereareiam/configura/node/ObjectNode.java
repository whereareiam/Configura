package me.whereareiam.configura.node;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@SuppressWarnings("unused")
public final class ObjectNode implements Node {
	private final Map<String, Node> values;

	public ObjectNode() {
		this.values = new LinkedHashMap<>();
	}

	public ObjectNode(Map<String, Node> values) {
		this.values = values != null ? new LinkedHashMap<>(values) : new LinkedHashMap<>();
	}

	@Override
	public NodeType type() {
		return NodeType.OBJECT;
	}

	@Override
	public String asText() {
		return "";
	}
}
