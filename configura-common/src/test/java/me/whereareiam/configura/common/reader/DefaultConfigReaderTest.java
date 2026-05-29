package me.whereareiam.configura.common.reader;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConfigReaderTest {
	@Test
	void readNodeFromBytesReturnsParsedTree() {
		DefaultConfigReader reader = new DefaultConfigReader();

		JsonNode node = reader.readNode("name: svc\nport: 8080\n".getBytes());

		assertEquals("svc", node.path("name").asText());
		assertEquals(8080, node.path("port").asInt());
	}

	@Test
	void readEmptyBytesReturnsEmptyObjectNode() {
		DefaultConfigReader reader = new DefaultConfigReader();

		JsonNode node = reader.readNode(new byte[0]);

		assertTrue(node.isObject());
		assertTrue(node.isEmpty());
	}
}
