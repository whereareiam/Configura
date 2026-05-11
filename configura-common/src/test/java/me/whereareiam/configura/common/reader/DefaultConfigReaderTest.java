package me.whereareiam.configura.common.reader;

import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConfigReaderTest {
	@Test
	void withFormatReturnsNewReader() {
		ConfigReader reader = new DefaultConfigReader();

		ConfigReader updated = reader.withFormat(Format.JSON);

		assertNotSame(reader, updated);
		assertEquals(Format.YAML, reader.getFormat());
		assertEquals(Format.JSON, updated.getFormat());
		assertTrue(updated instanceof DefaultConfigReader);
	}
}
