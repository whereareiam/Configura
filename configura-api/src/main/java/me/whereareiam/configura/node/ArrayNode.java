package me.whereareiam.configura.node;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuppressWarnings("unused")
public final class ArrayNode implements Node {
	private final List<Node> values;

	public ArrayNode() {
		this.values = new ArrayList<>();
	}

	public ArrayNode(List<Node> values) {
		this.values = values != null ? new ArrayList<>(values) : new ArrayList<>();
	}

	@Override
	public NodeType type() {
		return NodeType.ARRAY;
	}

	@Override
	public String asText() {
		return "";
	}
}
