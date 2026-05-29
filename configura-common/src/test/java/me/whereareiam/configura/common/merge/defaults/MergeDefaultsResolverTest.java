package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.MergeEngine;
import me.whereareiam.configura.common.merge.TestMergeProperties;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategyRegistry;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MergeDefaultsResolverTest {
	static final class TestDefaultStrategy implements FieldMergeStrategy {
		@Override
		public com.fasterxml.jackson.databind.JsonNode merge(@NonNull MergeContext context) {
			return context.mergeChildren(context.getSourceNode(), context.getDefaultNode());
		}
	}

	public static class Foo {
		public String value;
	}

	public static class FooProvider implements MergeDefaultsProvider<Foo> {
		@Override
		public Foo supply(Foo foo) {
			foo.value = "foo";
			return foo;
		}
	}

	static class Holder {
		@Defaults(text = "hello")
		public String s;

		@Defaults(source = @Defaults.Source("classpath:/nonexistent.json"))
		public String src;

		@Defaults(provider = @Defaults.Provider(FooProvider.class))
		public Foo foo;
	}

	@Test
	void resolvesProviderDefaults() {
		Holder holder = defaults(new Holder());
		assertNotNull(holder.foo);
		assertEquals("foo", holder.foo.value);
	}

	@Test
	void resolvesTextLiteral() {
		Holder holder = defaults(new Holder());
		assertEquals("hello", holder.s);
	}

	@Test
	void ignoresMissingDefaultsResource() {
		Holder holder = defaults(new Holder());
		assertNull(holder.src);
	}

	@SuppressWarnings("unchecked")
	private static <T> T defaults(T value) {
		ObjectMapper mapper = new ObjectMapper();
		MergeEngine engine = new MergeEngine(
				mapper,
				new MergeDefaultsProviderRegistry(),
				FieldMergeStrategyRegistry.standard(),
				TestMergeProperties.pluginRegistry(),
				TestMergeProperties.policyResolverRegistry(),
				TestDefaultStrategy.class,
				MergeBehavior.defaults()
		);
		try {
			return (T) mapper.treeToValue(
					engine.defaultsNode(value, (Class<T>) value.getClass()),
					value.getClass()
			);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
