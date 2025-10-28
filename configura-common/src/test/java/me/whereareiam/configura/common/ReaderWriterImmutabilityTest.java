package me.whereareiam.configura.common;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;

public class ReaderWriterImmutabilityTest {
	public static class T {
	}

	public static class TA implements TypeAdapter<T> {
		public T deserialize(String value) {
			return new T();
		}

		public String serialize(T value) {
			return "x";
		}
	}

	@Test
	void readerIsImmutable() {
		ConfigReader r0 = new DefaultConfigReader();
		ConfigReader r1 = r0.withFormat(Format.JSON);
		assertNotSame(r0, r1);
		ConfigReader r2 = r1.registerAdapter(T.class, TA.class);
		assertNotSame(r1, r2);
	}

	@Test
	void writerIsImmutable() {
		ConfigWriter w0 = new DefaultConfigWriter();
		ConfigWriter w1 = w0.withFormat(Format.JSON);
		assertNotSame(w0, w1);
		ConfigWriter w2 = w1.registerAdapter(T.class, TA.class);
		assertNotSame(w1, w2);
	}
}


