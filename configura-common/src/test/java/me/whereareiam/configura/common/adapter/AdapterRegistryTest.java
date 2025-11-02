package me.whereareiam.configura.common.adapter;

import me.whereareiam.configura.TypeAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdapterRegistryTest {
	static class T {
	}

	static class TA implements TypeAdapter<T> {
		public T deserialize(String value) {
			return new T();
		}

		public String serialize(T value) {
			return "t";
		}
	}

	@Test
	void withAdapterCreatesNewRegistry() {
		AdapterRegistry r1 = AdapterRegistry.empty();
		AdapterRegistry r2 = r1.withAdapter(T.class, TA.class);
		assertNotEquals(r1, r2);
		assertTrue(r2.asClassMap().containsKey(T.class));
	}
}


