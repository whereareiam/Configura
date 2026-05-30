package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.document.DefaultDocumentProcessor;
import me.whereareiam.configura.common.merge.MergeEngine;
import me.whereareiam.configura.common.merge.TestMergeProperties;
import me.whereareiam.configura.document.DocumentProcessor;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategyRegistry;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.List;

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

	public static class FooProvider implements DefaultsProvider<Foo> {
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

	interface ServiceContract {
	}

	static class ServiceDefaults implements ServiceContract {
		@Defaults(text = "https")
		public String scheme;
	}

	static class ServiceHolder {
		public ServiceContract service;
	}

	static class ChildWithoutNoArgs {
		@Defaults(text = "fallback")
		public String value;

		ChildWithoutNoArgs(String value) {
			this.value = value;
		}
	}

	static class ParentWithFieldOnlyChildDefaults {
		public ChildWithoutNoArgs child;
	}

	static class ListEntry {
		@Defaults(text = "list-default")
		public String name;
	}

	static class ListHolder {
		public List<ListEntry> entries;
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

	@Test
	void resolveInternalReturnsNullForUnsupportedType() {
		assertNull(resolver().resolveInternal(String.class, PrimitiveDefaultPolicy.PRESERVE));
	}

	@Test
	void resolvesFieldDefaultsWithoutNoArgsConstructor() {
		ObjectNode defaults = resolver().resolveInternal(
				ParentWithFieldOnlyChildDefaults.class,
				PrimitiveDefaultPolicy.PRESERVE,
				null
		);

		assertNotNull(defaults);
		assertEquals("fallback", defaults.path("child").path("value").asText());
	}

	@Test
	void resolvesModelDefaultsAgainstEffectiveDocumentType() {
		DocumentProcessor processor = new DefaultDocumentProcessor(
				List.of((declaredType, context) ->
						declaredType == ServiceContract.class ? ServiceDefaults.class : null),
				List.of()
		);
		ObjectNode defaults = resolver(processor).resolveInternal(
				ServiceHolder.class,
				PrimitiveDefaultPolicy.PRESERVE,
				null
		);

		assertNotNull(defaults);
		assertEquals("https", defaults.path("service").path("scheme").asText());
	}

	@Test
	void doesNotWriteObjectDefaultsToListField() {
		ObjectNode defaults = resolver().resolveInternal(
				ListHolder.class,
				PrimitiveDefaultPolicy.PRESERVE,
				null
		);

		assertNull(defaults);
	}

	@SuppressWarnings("unchecked")
	private static <T> T defaults(T value) {
		ObjectMapper mapper = new ObjectMapper();
		MergeEngine engine = new MergeEngine(
				mapper,
				new DefaultsProviderRegistry(),
				new DefaultDocumentProcessor(List.of(), List.of()),
				asDefinitions(),
				TestMergeProperties.adapterRegistry(),
				TestMergeProperties.defaultsResolverRegistry(),
				TestMergeProperties.policyResolverRegistry(),
				TestDefaultStrategy.class,
				null,
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

	private static MergeDefaultsResolver resolver() {
		return resolver(new DefaultDocumentProcessor(List.of(), List.of()));
	}

	private static MergeDefaultsResolver resolver(DocumentProcessor documentProcessor) {
		return new MergeDefaultsResolver(
				new ObjectMapper(),
				new DefaultsProviderRegistry(),
				documentProcessor,
				TestMergeProperties.defaultsResolverRegistry(),
				TestMergeProperties.adapterRegistry(),
				TestMergeProperties.policyResolverRegistry()
		);
	}

	private static me.whereareiam.configura.merge.strategy.MergeStrategyRegistry asDefinitions() {
		me.whereareiam.configura.merge.strategy.MergeStrategyRegistry registry =
				new me.whereareiam.configura.merge.strategy.MergeStrategyRegistry();
		FieldMergeStrategyRegistry.standard().asMap().forEach(registry::registerAlias);
		return registry;
	}
}
