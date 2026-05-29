package me.whereareiam.configura.merge.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.merge.annotation.Merge;
import me.whereareiam.configura.merge.annotation.MergeList;
import me.whereareiam.configura.merge.plugin.context.MergePluginContext;
import me.whereareiam.configura.merge.plugin.context.MergePluginDefaultsContext;
import me.whereareiam.configura.merge.plugin.descriptor.MergeDescriptor;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.policy.MergePolicyResolver;
import me.whereareiam.configura.merge.strategy.NeverDefaults;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.type.merge.tree.list.ListMode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Merge Field Plugin Integration")
class MergePluginIntegrationTest {
	@Test
	@DisplayName("Custom field plugin participates in defaults expansion and recursive merge")
	void customFieldPluginParticipatesInDefaultsExpansionAndRecursiveMerge(@TempDir Path tempDir) throws Exception {
		Configura configura = Config.builder()
				.format(Format.YAML)
				.policyResolver(new EnvelopePolicyResolver())
				.mergePlugin(new EnvelopePlugin())
				.build();

		Path file = tempDir.resolve("envelope.yml");
		Files.writeString(file, """
				envelope:
				  value:
				    name: custom
				""");

		EnvelopeConfig config = configura.update(file, EnvelopeConfig.class);

		assertNotNull(config.envelope);
		assertNotNull(config.envelope.value);
		assertEquals("custom", config.envelope.value.name);
		assertTrue(config.envelope.value.enabled);
		assertTrue(Files.readString(file).contains("enabled: true"));
	}

	@Test
	@DisplayName("User plugin registration overrides the built-in field plugin")
	void userPluginRegistrationOverridesBuiltInFieldPlugin(@TempDir Path tempDir) {
		Configura configura = Config.builder()
				.format(Format.YAML)
				.mergePlugin(new OverrideListPlugin())
				.build();

		OverrideListConfig config = configura.update(tempDir.resolve("override-list"), OverrideListConfig.class);

		assertEquals(List.of("override"), config.items);
	}

	@Test
	@DisplayName("User policy resolver overrides annotation strategy helpers")
	void userPolicyResolverOverridesAnnotationStrategyHelpers(@TempDir Path tempDir) {
		Configura configura = Config.builder()
				.format(Format.YAML)
				.policyResolver(new NeverDefaultsPolicyResolver())
				.build();

		StrategyOverrideConfig config = configura.update(tempDir.resolve("override-strategy"), StrategyOverrideConfig.class);

		assertNull(config.mode);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	private @interface MergeEnvelope {
		String key() default "value";
	}

	private static final class EnvelopePluginConfig {
		private final String key;

		private EnvelopePluginConfig(String key) {
			this.key = key;
		}
	}

	private static final class EnvelopePolicyResolver implements MergePolicyResolver {
		@Override
		public MergePolicy resolve(@NotNull MergeDescriptor descriptor) {
			if (descriptor.getField() == null) return null;
			MergeEnvelope annotation = descriptor.getField().getAnnotation(MergeEnvelope.class);
			if (annotation == null) return null;
			return MergePolicy.builder()
					.helper(EnvelopePluginConfig.class, new EnvelopePluginConfig(annotation.key()))
					.build();
		}
	}

	private static final class EnvelopePlugin implements MergePlugin {
		@Override
		public boolean supports(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
			return descriptor.getDeclaredType() == Envelope.class && policy.hasHelper(EnvelopePluginConfig.class);
		}

		@Override
		public @NotNull Class<?> resolveChildType(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
			return EnvelopeValue.class;
		}

		@Override
		public JsonNode resolveDefaultValue(@NotNull MergePluginDefaultsContext context) {
			EnvelopePluginConfig config = context.getPolicy().helper(EnvelopePluginConfig.class);
			JsonNode childDefaults = context.resolveModelDefaults(context.getChildType());
			if (childDefaults == null) return null;
			ObjectNode result = context.getMapper().createObjectNode();
			result.set(config.key, childDefaults.deepCopy());
			return result;
		}

		@Override
		public @NotNull JsonNode merge(@NotNull MergePluginContext context) {
			EnvelopePluginConfig config = context.getPolicy().helper(EnvelopePluginConfig.class);
			ObjectNode result = context.getMapper().createObjectNode();
			JsonNode sourceChild = context.getSourceNode() != null && context.getSourceNode().isObject()
					? context.getSourceNode().get(config.key)
					: null;
			JsonNode defaultChild = context.getDefaultNode() != null && context.getDefaultNode().isObject()
					? context.getDefaultNode().get(config.key)
					: null;
			result.set(config.key, context.mergeChildren(sourceChild, defaultChild));
			return result;
		}
	}

	private static final class OverrideListPlugin implements MergePlugin {
		@Override
		public boolean supports(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
			return descriptor.getField() != null && List.class.isAssignableFrom(descriptor.getField().getType());
		}

		@Override
		public @NotNull Class<?> resolveChildType(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
			return descriptor.getDeclaredType();
		}

		@Override
		public JsonNode resolveDefaultValue(@NotNull MergePluginDefaultsContext context) {
			ArrayNode array = context.getMapper().createArrayNode();
			array.add("override");
			return array;
		}

		@Override
		public @NotNull JsonNode merge(@NotNull MergePluginContext context) {
			ArrayNode array = context.getMapper().createArrayNode();
			array.add("override");
			return array;
		}
	}

	private static final class NeverDefaultsPolicyResolver implements MergePolicyResolver {
		@Override
		public MergePolicy resolve(@NotNull MergeDescriptor descriptor) {
			if (!"mode".equals(descriptor.getSerializedName())) return null;
			return MergePolicy.builder()
					.strategy(NeverDefaults.class)
					.build();
		}
	}

	private static final class EnvelopeConfig {
		@MergeEnvelope
		public Envelope envelope;
	}

	private static final class Envelope {
		public EnvelopeValue value;
	}

	private static final class EnvelopeValue {
		@Defaults(text = "default-name")
		public String name;

		@Defaults(bool = true)
		public boolean enabled;
	}

	private static final class OverrideListConfig {
		@Defaults(stringItems = {"base"})
		@MergeList(mode = ListMode.PLAIN)
		public List<String> items;
	}

	private static final class StrategyOverrideConfig {
		@Defaults(text = "default-mode")
		@Merge(SourceOwnsField.class)
		public String mode;
	}
}
