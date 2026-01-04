package me.whereareiam.configura.node;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class StringNode implements Node {
	private final String value;

	@Override
	public NodeType type() {
		return NodeType.STRING;
	}

	@Override
	public String asText() {
		return value;
	}
}
