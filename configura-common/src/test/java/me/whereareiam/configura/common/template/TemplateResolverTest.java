package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.annotation.template.Source;
import me.whereareiam.configura.annotation.template.Supplier;
import me.whereareiam.configura.annotation.template.Template;
import me.whereareiam.configura.annotation.template.type.Literal;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateResolverTest {
	public static class Foo {
	}

	public static class FooSupplier implements TemplateProvider<Foo> {
		@Override
		public Foo supply(Foo foo) {
			return foo;
		}
	}

	static class Holder {
		@Template(literal = @Literal(text = "hello"))
		public String s;

		@Source("classpath:/nonexistent.json")
		public String src;

		@Supplier(FooSupplier.class)
		public Foo foo;
	}

	@Test
	void supplierPrecedenceOverOthers() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Field f = Holder.class.getDeclaredField("foo");
		Object o = TemplateResolver.resolveFieldTemplate(mapper, Foo.class, f);
		assertNotNull(o);
		assertInstanceOf(Foo.class, o);
	}

	@Test
	void literalLiteralApplies() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Field f = Holder.class.getDeclaredField("s");
		Object o = TemplateResolver.resolveFieldTemplate(mapper, String.class, f);
		assertEquals("hello", o);
	}

	@Test
	void missingResourceIgnored() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Field f = Holder.class.getDeclaredField("src");
		Object o = TemplateResolver.resolveFieldTemplate(mapper, String.class, f);
		assertNull(o);
	}
}


