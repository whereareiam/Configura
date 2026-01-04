package me.whereareiam.configura.node;

public final class NullNode implements Node {
	private static final NullNode INSTANCE = new NullNode();

	public static NullNode instance() {
		return INSTANCE;
	}

	@Override
	public NodeType type() {
		return NodeType.NULL;
	}

	@Override
	public String asText() {
		return "null";
	}
}
