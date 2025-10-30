package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.adapter.AdapterModule;
import me.whereareiam.configura.common.polymorphic.PolymorphicModule;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateInjectionModifierTest {
	static class A {
		@Field
		@Template(text = "x")
		public String v;
	}

	@Test
	void appliesTemplateWhenMissing() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new TemplateModule());
		mapper.registerModule(new AdapterModule());
		mapper.registerModule(new PolymorphicModule());

		A a = mapper.convertValue(new HashMap<>(), A.class);

		assertEquals("x", a.v);
	}
}