package me.whereareiam.configura.node;

public final class BooleanNode implements Node {
	private final boolean value;

	public BooleanNode(boolean value) {
		this.value = value;
	}

	public boolean getValue() {
		return value;
	}

	@Override
	public NodeType type() {
		return NodeType.BOOLEAN;
	}

	@Override
	public String asText() {
		return String.valueOf(value);
	}
}
