package me.whereareiam.configura.common;

import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.template.DefaultTemplateRegistry;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;

public class ReaderWriterImmutabilityTest {
	@Test
	void readerIsImmutable() {
		ConfigReader r0 = new DefaultConfigReader();
		ConfigReader r1 = r0.withFormat(Format.JSON);
		assertNotSame(r0, r1);
		ConfigReader r2 = r1.withTemplateRegistry(new DefaultTemplateRegistry());
		assertNotSame(r1, r2);
	}

	@Test
	void writerIsImmutable() {
		ConfigWriter w0 = new DefaultConfigWriter();
		ConfigWriter w1 = w0.withFormat(Format.JSON);
		assertNotSame(w0, w1);
		ConfigWriter w2 = w1.withTemplateRegistry(new DefaultTemplateRegistry());
		assertNotSame(w1, w2);
	}
}

