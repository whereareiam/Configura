package me.whereareiam.configura.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class PathNavigator {
	private PathNavigator() {
	}

	public static boolean has(ObjectNode node, String path) {
		String[] parts = path.split("\\.");
		JsonNode current = node;

		for (String part : parts) {
			if (!current.isObject()) return false;

			current = current.get(part);
			if (current == null || current.isNull()) return false;
		}

		return true;
	}

	public static void write(ObjectNode node, String path, JsonNode value) {
		String[] parts = path.split("\\.");
		ObjectNode current = node;

		for (int i = 0; i < parts.length - 1; i++) {
			String part = parts[i];
			JsonNode existing = current.get(part);
			if (existing == null || !existing.isObject()) {
				ObjectNode child = current.objectNode();
				current.set(part, child);
				current = child;
				continue;
			}

			current = (ObjectNode) existing;
		}

		current.set(parts[parts.length - 1], value);
	}

	public static JsonNode read(ObjectNode node, String path) {
		String[] parts = path.split("\\.");
		JsonNode current = node;

		for (String part : parts) {
			if (!current.isObject()) return null;

			current = current.get(part);
			if (current == null) return null;
		}

		return current;
	}
}



