package me.whereareiam.configura.common.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.template.Template;
import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.common.ConfiguraModule;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldDefaultsModifierTest {
	static class A {
		@Field
		@Template(literal = @Literal(text = "x"))
		public String v;
	}

	@Test
	void appliesTemplateWhenMissing() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new ConfiguraModule(Collections.emptyMap()));
		A a = mapper.convertValue(new HashMap<>(), A.class);
		assertEquals("x", a.v);
	}
}
