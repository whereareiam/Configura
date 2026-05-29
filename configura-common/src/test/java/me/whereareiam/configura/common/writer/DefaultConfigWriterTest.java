package me.whereareiam.configura.common.writer;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConfigWriterTest {
	@Test
	void writeNodeBytesSerializesTree() {
		DefaultConfigWriter writer = new DefaultConfigWriter();

		byte[] bytes = writer.writeNodeBytes(JsonNodeFactory.instance.objectNode().put("name", "svc"));

		assertTrue(bytes.length > 0);
	}
}
