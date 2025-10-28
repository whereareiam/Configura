package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.template.Template;
import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.annotation.template.type.Property;
import me.whereareiam.configura.common.ConfiguraModule;
import org.junit.jupiter.api.Test;

import java.util.Collections;
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
		@Template(items = {@Literal(text = "a"), @Literal(text = "b")})
		public List<String> names;

		@Field
		@Template(properties = {@Property(name = "retries", value = @Literal(number = "3"))})
		public RetryPolicy policy;
	}

	@Test
	void listLiteralBindsViaModifier() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new ConfiguraModule(Collections.emptyMap()));

		Holder holder = mapper.convertValue(new HashMap<>(), Holder.class);
		assertNotNull(holder.names);
		assertEquals(List.of("a", "b"), holder.names);
	}

	@Test
	void objectLiteralBindsPartialViaModifier() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new ConfiguraModule(Collections.emptyMap()));

		Holder holder = mapper.convertValue(new HashMap<>(), Holder.class);
		assertNotNull(holder.policy);
		assertEquals(3, holder.policy.retries);
		assertNull(holder.policy.backoff);
	}
}


