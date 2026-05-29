package me.whereareiam.configura.common.polymorphic;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.whereareiam.configura.annotation.Polymorphic;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.type.Format;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PolymorphicIntegrationTest {
	@Getter
	@ToString
	@NoArgsConstructor
	@SuperBuilder(toBuilder = true)
	@Polymorphic(
			discriminator = "type",
			mappings = {
					@Polymorphic.Type(value = "SYMBOL", target = SymbolTrigger.class),
					@Polymorphic.Type(value = "REGEX", target = RegexTrigger.class),
					@Polymorphic.Type(value = "COMMAND", target = CommandTrigger.class)
			},
			defaultValue = "SYMBOL"
	)
	public static class TriggerBase {
		protected String type;
		protected boolean strip;
		protected Integer radius;
	}

	@Getter
	@ToString(callSuper = true)
	@SuperBuilder(toBuilder = true)
	@NoArgsConstructor
	public static class SymbolTrigger extends TriggerBase {
		private String symbol;
	}

	@Getter
	@ToString(callSuper = true)
	@SuperBuilder(toBuilder = true)
	@NoArgsConstructor
	public static class RegexTrigger extends TriggerBase {
		private String pattern;
	}

	@Getter
	@ToString(callSuper = true)
	@SuperBuilder(toBuilder = true)
	@NoArgsConstructor
	public static class CommandTrigger extends TriggerBase {
		private String command;
	}

	public static class InferBase {
	}

	public static class InferServers extends InferBase {
		public List<String> servers;
	}

	public static class InferWorlds extends InferBase {
		public List<String> worlds;
	}

	public static class BuilderBase {
		public String type;
		public Integer radius;
	}

	public static class BuilderSymbol extends BuilderBase {
		public String symbol;
	}

	public static class BuilderRegex extends BuilderBase {
		public String pattern;
	}

	public static class BuilderCommand extends BuilderBase {
		public String command;
	}

	@Test
	void readsSymbolVariantFromYamlAnnotation() {
		String yaml = """
				type: SYMBOL
				symbol: "#"
				strip: true
				radius: 10
				""";
		TriggerBase t = new DefaultConfigReader().read(yaml.getBytes(), TriggerBase.class);
		assertInstanceOf(SymbolTrigger.class, t);
		assertEquals("#", ((SymbolTrigger) t).getSymbol());
	}

	@Test
	void writesOnlyRelevantFieldsJsonAnnotation() {
		RegexTrigger t = RegexTrigger.builder()
				.type("REGEX")
				.pattern("^hi")
				.strip(false)
				.radius(null)
				.build();

		byte[] out;
		try {
			out = MapperFactory.buildWriterMapper(Format.JSON).writeValueAsBytes(t);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		String text = new String(out);

		assertTrue(text.contains("type"));
		assertTrue(text.contains("REGEX"));
		assertTrue(text.contains("pattern"));
		assertFalse(text.contains("symbol"));
		assertFalse(text.contains("command"));
	}

	@Test
	void readsCommandVariantWithBuilderMappingBuilder() {
		PolymorphicRegistry.register(BuilderBase.class)
				.discriminator("type")
				.map("SYMBOL", BuilderSymbol.class)
				.map("REGEX", BuilderRegex.class)
				.map("COMMAND", BuilderCommand.class)
				.defaultValue("SYMBOL")
				.build();

		String yaml = """
				type: COMMAND
				command: reply
				radius: 0
				""";

		BuilderBase t = new DefaultConfigReader().read(yaml.getBytes(), BuilderBase.class);
		assertInstanceOf(BuilderCommand.class, t);
		assertEquals("reply", ((BuilderCommand) t).command);
	}

	@Test
	void readsWithBuilderInferenceOnly() {
		PolymorphicRegistry.register(InferBase.class)
				.inferByField("servers", InferServers.class)
				.inferByField("worlds", InferWorlds.class)
				.build();

		String yaml = """
				servers: ["s1", "s2"]
				""";
		InferBase v = new DefaultConfigReader().read(yaml.getBytes(), InferBase.class);
		assertInstanceOf(InferServers.class, v);
	}
}
