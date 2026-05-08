package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.annotation.Template;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class TemplateResolutionTest {
	public static class Foo {
	}

	public static class FooSupplier implements TemplateProvider<Foo> {
		@Override
		public Foo supply(Foo foo) {
			return foo;
		}
	}

	static class Holder {
		@Template(text = "hello")
		public String s;

		@Template(source = @Template.Source("classpath:/nonexistent.json"))
		public String src;

		@Template(supplier = @Template.Supplier(FooSupplier.class))
		public Foo foo;
	}

	@Test
	void resolvesSupplierTemplate() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Field f = Holder.class.getDeclaredField("foo");
		Object o = TemplateResolver.resolveFieldTemplate(mapper, null, Foo.class, f);
		assertNotNull(o);
		assertInstanceOf(Foo.class, o);
	}

	@Test
	void resolvesTextLiteral() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Field f = Holder.class.getDeclaredField("s");
		Object o = TemplateResolver.resolveFieldTemplate(mapper, null, String.class, f);
		assertEquals("hello", o);
	}

	@Test
	void ignoresMissingTemplateResource() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Field f = Holder.class.getDeclaredField("src");
		Object o = TemplateResolver.resolveFieldTemplate(mapper, null, String.class, f);
		assertNull(o);
	}
}
