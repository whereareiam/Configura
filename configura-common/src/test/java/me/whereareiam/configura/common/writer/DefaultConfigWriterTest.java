package me.whereareiam.configura.common.writer;

import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConfigWriterTest {
	@Test
	void withFormatReturnsNewWriter() {
		ConfigWriter writer = new DefaultConfigWriter();

		ConfigWriter updated = writer.withFormat(Format.JSON);

		assertNotSame(writer, updated);
		assertEquals(Format.YAML, writer.getFormat());
		assertEquals(Format.JSON, updated.getFormat());
		assertTrue(updated instanceof DefaultConfigWriter);
	}
}
