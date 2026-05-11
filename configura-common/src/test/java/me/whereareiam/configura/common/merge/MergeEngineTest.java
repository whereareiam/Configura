package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.defaults.DefaultMergeDefaultsRegistry;
import me.whereareiam.configura.merge.MergeStrategyRegistry;
import me.whereareiam.configura.merge.strategy.DeepDefaults;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MergeEngineTest {
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

	@SuppressWarnings("unchecked")
	private static <T> T defaults(T holder) {
		ObjectMapper mapper = new ObjectMapper();
		MergeEngine engine = new MergeEngine(
				mapper,
				new DefaultMergeDefaultsRegistry(),
				MergeStrategyRegistry.standard(),
				DeepDefaults.class
		);
		try {
			return (T) mapper.treeToValue(
					engine.defaultsNode(holder, (Class<T>) holder.getClass(), MergeEngine.Mode.USER_MODEL),
					holder.getClass()
			);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
