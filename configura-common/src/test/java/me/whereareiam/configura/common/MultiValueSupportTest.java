package me.whereareiam.configura.common;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.type.MultiValue;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiValueSupportTest {
	@Setter
	@Getter
	static class Conf {
		private MultiValue<String> audit;
	}

	@Test
	void deserializesScalarToMultiValue() {
		String yaml = "audit: \"123\"\n";
		DefaultConfigReader reader = (DefaultConfigReader) new DefaultConfigReader().withFormat(Format.YAML);

		Conf conf = reader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), Conf.class);

		assertEquals(List.of("123"), conf.getAudit().asList());
	}

	@Test
	void deserializesArrayToMultiValue() {
		String yaml = "audit:\n  - \"123\"\n  - \"456\"\n";
		DefaultConfigReader reader = (DefaultConfigReader) new DefaultConfigReader().withFormat(Format.YAML);

		Conf conf = reader.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), Conf.class);

		assertEquals(List.of("123", "456"), conf.getAudit().asList());
	}

	@Test
	void serializesAsArray() {
		Conf conf = new Conf();
		conf.setAudit(MultiValue.of(List.of("123", "456")));

		DefaultConfigWriter writer = (DefaultConfigWriter) new DefaultConfigWriter().withFormat(Format.YAML);
		byte[] out = writer.encode(conf);

		String yaml = new String(out, StandardCharsets.UTF_8);
		assertTrue(yaml.contains("- \"123\""));
		assertTrue(yaml.contains("- \"456\""));
	}

	@Test
	void serializesSingleAsScalar() {
		Conf conf = new Conf();
		conf.setAudit(MultiValue.of("123"));

		DefaultConfigWriter writer = (DefaultConfigWriter) new DefaultConfigWriter().withFormat(Format.YAML);
		byte[] out = writer.encode(conf);

		String yaml = new String(out, StandardCharsets.UTF_8);
		assertTrue(yaml.contains("audit: \"123\""));
	}
}
