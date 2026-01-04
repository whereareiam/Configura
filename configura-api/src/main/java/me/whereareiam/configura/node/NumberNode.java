package me.whereareiam.configura.node;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class NumberNode implements Node {
	private final Number value;

	@Override
	public NodeType type() {
		return NodeType.NUMBER;
	}

	@Override
	public String asText() {
		return value != null ? String.valueOf(value) : null;
	}
}
