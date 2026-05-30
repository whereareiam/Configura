package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.annotation.PreserveUnknownFields;
import me.whereareiam.configura.common.document.DefaultDocumentProcessor;
import me.whereareiam.configura.common.merge.defaults.DefaultsProviderRegistry;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategyRegistry;
import me.whereareiam.configura.type.UnknownFieldPolicy;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MergeEngineTest {
	static final class TestDefaultStrategy implements FieldMergeStrategy {
		@Override
		public com.fasterxml.jackson.databind.JsonNode merge(@NonNull MergeContext context) {
			return context.mergeChildren(context.getSourceNode(), context.getDefaultNode());
		}
	}

	static class Retry {
		public int retries;
		public Backoff backoff;
	}

	static class Backoff {
		public long initialMs;
		public long maxMs;
	}

	static class SimpleHolder {
		@Defaults(text = "x")
		public String value;
	}

	static class PlainHolder {
		public String value;
	}

	@PreserveUnknownFields
	static class PreservingHolder {
		public String value;
	}

	static class SeedHolder {
		@Defaults(stringItems = {"a", "b"})
		public List<String> names;

		@Defaults(properties = {
				@Defaults.Property(name = "retries", number = "3")
		})
		public Retry policy;
	}

	@Test
	void defaultsNodeAppliesTextDefaultsWhenValueIsMissing() {
		SimpleHolder holder = new SimpleHolder();
		holder.value = null;

		holder = defaults(holder);

		assertEquals("x", holder.value);
	}

	@Test
	void defaultsNodeBindsListLiteral() {
		SeedHolder holder = new SeedHolder();
		holder.names = null;

		holder = defaults(holder);

		assertNotNull(holder.names);
		assertEquals(List.of("a", "b"), holder.names);
	}

	@Test
	void defaultsNodeBindsObjectLiteralPartially() {
		SeedHolder holder = new SeedHolder();
		holder.policy = null;

		holder = defaults(holder);

		assertNotNull(holder.policy);
		assertEquals(3, holder.policy.retries);
		assertNull(holder.policy.backoff);
	}

	@Test
	void mergeDropsUnknownFieldsByDefault() {
		ObjectMapper mapper = new ObjectMapper();
		MergeEngine engine = engine(mapper);

		SimpleHolder merged = bind(mapper, engine.mergeUserModel(
				mapper.createObjectNode().put("value", "user").put("legacy", "keep?"),
				new SimpleHolder(),
				SimpleHolder.class
		), SimpleHolder.class);

		assertEquals("user", merged.value);
		assertFalse(mapper.valueToTree(merged).has("legacy"));
	}

	@Test
	void mergePreservesUnknownFieldsForAnnotatedClass() {
		ObjectMapper mapper = new ObjectMapper();
		MergeEngine engine = engine(mapper);

		assertTrue(engine.mergeUserModel(
				mapper.createObjectNode().put("value", "user").put("legacy", "keep"),
				new PreservingHolder(),
				PreservingHolder.class
		).has("legacy"));
	}

	@Test
	void mergePreservesUnknownFieldsForInstanceBehavior() {
		ObjectMapper mapper = new ObjectMapper();
		MergeEngine engine = engine(
				mapper,
				MergeBehavior.builder()
						.unknownFields(UnknownFieldPolicy.PRESERVE)
						.build()
		);

		assertTrue(engine.mergeUserModel(
				mapper.createObjectNode().put("value", "user").put("legacy", "keep"),
				new SimpleHolder(),
				SimpleHolder.class
		).has("legacy"));
	}

	@Test
	void mergeKeepsDeclaredFieldsWithoutDefaults() {
		ObjectMapper mapper = new ObjectMapper();
		MergeEngine engine = engine(mapper);

		PlainHolder merged = bind(mapper, engine.mergeUserModel(
				mapper.createObjectNode().put("value", "user").put("legacy", "drop"),
				new PlainHolder(),
				PlainHolder.class
		), PlainHolder.class);

		assertEquals("user", merged.value);
	}

	@SuppressWarnings("unchecked")
	private static <T> T defaults(T holder) {
		ObjectMapper mapper = new ObjectMapper();
		MergeEngine engine = engine(mapper);
		return bind(mapper, engine.defaultsNode(holder, (Class<T>) holder.getClass()), (Class<T>) holder.getClass());
	}

	private static MergeEngine engine(ObjectMapper mapper) {
		return engine(mapper, MergeBehavior.defaults());
	}

	private static MergeEngine engine(ObjectMapper mapper, MergeBehavior behavior) {
		return new MergeEngine(
				mapper,
				new DefaultsProviderRegistry(),
				new DefaultDocumentProcessor(List.of(), List.of()),
				asDefinitions(),
				TestMergeProperties.adapterRegistry(),
				TestMergeProperties.defaultsResolverRegistry(),
				TestMergeProperties.policyResolverRegistry(),
				TestDefaultStrategy.class,
				null,
				behavior
		);
	}

	private static me.whereareiam.configura.merge.strategy.MergeStrategyRegistry asDefinitions() {
		me.whereareiam.configura.merge.strategy.MergeStrategyRegistry registry =
				new me.whereareiam.configura.merge.strategy.MergeStrategyRegistry();
		FieldMergeStrategyRegistry.standard().asMap().forEach(registry::registerAlias);
		return registry;
	}

	private static <T> T bind(ObjectMapper mapper, com.fasterxml.jackson.databind.JsonNode node, Class<T> type) {
		try {
			return mapper.treeToValue(node, type);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
