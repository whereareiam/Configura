package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.Template;
import org.junit.jupiter.api.Test;

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
	void listLiteralBindsViaSeeder() {
		ObjectMapper mapper = new ObjectMapper();
		Holder holder = new Holder();
		holder.names = null;
		new TemplateSeeder(mapper, null).seed(holder);
		assertNotNull(holder.names);
		assertEquals(List.of("a", "b"), holder.names);
	}

	@Test
	void objectLiteralBindsPartialViaSeeder() {
		ObjectMapper mapper = new ObjectMapper();
		Holder holder = new Holder();
		holder.policy = null;
		new TemplateSeeder(mapper, null).seed(holder);
		assertNotNull(holder.policy);
		assertEquals(3, holder.policy.retries);
		assertNull(holder.policy.backoff);
	}
}