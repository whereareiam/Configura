package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.adapter.AdapterModule;
import me.whereareiam.configura.common.polymorphic.PolymorphicModule;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateResolverLiteralsTest {
	static class RetryPolicy {
		public int retries;
		public Backoff backoff;
	}

	static class Backoff {
		public long initialMs;
		public long maxMs;
	}

	static class Holder {
		@Field
		@Template(stringItems = {"a", "b"})
		public List<String> names;

		@Field
		@Template(properties = {
				@Template.Property(name = "retries", number = "3")
		})
		public RetryPolicy policy;
	}

	@Test
	void listLiteralBindsViaModifier() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new TemplateModule());
		mapper.registerModule(new AdapterModule());
		mapper.registerModule(new PolymorphicModule());

		Holder holder = mapper.convertValue(new HashMap<>(), Holder.class);

		assertNotNull(holder.names);
		assertEquals(List.of("a", "b"), holder.names);
	}

	@Test
	void objectLiteralBindsPartialViaModifier() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new TemplateModule());
		mapper.registerModule(new AdapterModule());
		mapper.registerModule(new PolymorphicModule());

		Holder holder = mapper.convertValue(new HashMap<>(), Holder.class);

		assertNotNull(holder.policy);
		assertEquals(3, holder.policy.retries);
		assertNull(holder.policy.backoff);
	}
}


