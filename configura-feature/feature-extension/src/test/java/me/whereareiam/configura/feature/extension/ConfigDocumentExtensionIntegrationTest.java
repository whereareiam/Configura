package me.whereareiam.configura.feature.extension;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.PreserveUnknownFields;
import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.annotation.merge.MergeList;
import me.whereareiam.configura.annotation.merge.MergeMap;
import me.whereareiam.configura.feature.extension.api.ConfigDocumentRule;
import me.whereareiam.configura.feature.extension.api.annotation.ExtendableDocument;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.type.merge.tree.list.ListMode;
import me.whereareiam.configura.type.merge.tree.list.ListPresence;
import me.whereareiam.configura.type.merge.tree.list.ListUnknownEntries;
import me.whereareiam.configura.type.merge.tree.map.MapPresence;
import me.whereareiam.configura.type.merge.tree.map.MapUnknownEntries;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Config Document Extensions")
class ConfigDocumentExtensionIntegrationTest {
	@Test
	@DisplayName("Whole-document extensions bind subtypes and apply layered defaults")
	void wholeDocumentExtensionsBindSubtypesAndApplyLayeredDefaults(@TempDir Path tempDir) throws Exception {
		var configura = Config.builder()
				.format(Format.YAML)
				.defaults(SingleCommandDefaults.class)
				.defaults(CommandDefaults.class)
				.defaults(SecureCommandDefaults.class)
				.feature(ExtensionFeature.rule(
						ConfigDocumentRule.whole(CommandDefinition.class, SecureCommandDefinition.class)
				))
				.build();

		Path path = tempDir.resolve("single-command.yml");
		Files.writeString(path, """
				command:
				  description: "Login"
				""");

		SingleCommandDocument document = configura.update(path, SingleCommandDocument.class);

		assertInstanceOf(SecureCommandDefinition.class, document.command);
		assertTrue(document.command.enabled);
		assertEquals("PASSWORD", ((SecureCommandDefinition) document.command).inputMode);
		assertTrue(Files.readString(path).contains("inputMode: \"PASSWORD\""));
	}

	@Test
	@DisplayName("Map-key extensions only apply to matching entries")
	void mapKeyExtensionsOnlyApplyToMatchingEntries(@TempDir Path tempDir) throws Exception {
		var configura = Config.builder()
				.format(Format.YAML)
				.defaults(CommandMapDefaults.class)
				.defaults(CommandDefaults.class)
				.defaults(SecureCommandDefaults.class)
				.feature(ExtensionFeature.rule(ConfigDocumentRule.when(
						CommandDefinition.class,
						SecureCommandDefinition.class,
						context -> "login".equals(context.getMapKey())
				)))
				.build();

		Path path = tempDir.resolve("commands.yml");
		Files.writeString(path, """
				commands:
				  login: {}
				  info: {}
				""");

		CommandMapDocument document = configura.update(path, CommandMapDocument.class);

		assertInstanceOf(SecureCommandDefinition.class, document.commands.get("login"));
		assertEquals("PASSWORD", ((SecureCommandDefinition) document.commands.get("login")).inputMode);
		assertEquals(CommandDefinition.class, document.commands.get("info").getClass());
		String text = Files.readString(path);
		assertTrue(text.contains("login:"));
		assertTrue(text.contains("inputMode: \"PASSWORD\""));
	}

	@Test
	@DisplayName("Keyed-list extensions preserve fields when the extension is unavailable later")
	void keyedListExtensionsPreserveFieldsWhenExtensionIsUnavailableLater(@TempDir Path tempDir) throws Exception {
		Path path = tempDir.resolve("providers.yml");
		Files.writeString(path, """
				providers:
				  - id: corporate
				""");

		var extended = Config.builder()
				.format(Format.YAML)
				.defaults(ProviderListDefaults.class)
				.defaults(CorporateProviderDefaults.class)
				.feature(ExtensionFeature.rule(ConfigDocumentRule.when(
						ProviderEntry.class,
						CorporateProviderEntry.class,
						context -> {
							if (context.getCurrentNode() == null || !context.getCurrentNode().isObject()) return false;
							var value = context.getCurrentNode().get("id");
							return value != null && value.isValueNode() && "corporate".equals(value.asText());
						}
				)))
				.build();

		ProviderListDocument initial = extended.update(path, ProviderListDocument.class);
		assertInstanceOf(CorporateProviderEntry.class, initial.providers.getFirst());
		assertTrue(Files.readString(path).contains("domainWhitelist:"));

		var baseOnly = Config.builder()
				.format(Format.YAML)
				.defaults(ProviderListDefaults.class)
				.build();

		ProviderListDocument preserved = baseOnly.update(path, ProviderListDocument.class);
		assertEquals("corporate", preserved.providers.getFirst().id);
		assertTrue(baseOnly.readNode(path).path("providers").get(0).has("domainWhitelist"));
	}

	@Test
	@DisplayName("Field-level nested extensions resolve for non-annotated nested base types")
	void fieldLevelNestedExtensionsResolveForNonAnnotatedNestedBaseTypes(@TempDir Path tempDir) throws Exception {
		var configura = Config.builder()
				.format(Format.YAML)
				.defaults(NestedDefaults.class)
				.defaults(VerificationDefaults.class)
				.defaults(SmsVerificationDefaults.class)
				.feature(ExtensionFeature.rule(
						ConfigDocumentRule.whole(Verification.class, SmsVerification.class)
				))
				.build();

		Path path = tempDir.resolve("nested.yml");
		Files.writeString(path, """
				provider:
				  id: corporate
				  verification:
				    type: sms
				""");

		NestedProviderDocument document = configura.update(path, NestedProviderDocument.class);

		assertInstanceOf(SmsVerification.class, document.provider.verification);
		assertEquals("sms-default", ((SmsVerification) document.provider.verification).template);
		assertTrue(Files.readString(path).contains("template: \"sms-default\""));
	}

	static final class SingleCommandDocument {
		@ExtendableDocument
		public CommandDefinition command;
	}

	static final class CommandMapDocument {
		@Merge
		@MergeMap(presence = MapPresence.DECLARED_ONLY, unknownEntries = MapUnknownEntries.ALLOW)
		public Map<String, CommandDefinition> commands = new LinkedHashMap<>();
	}

	static final class ProviderListDocument {
		@Merge
		@MergeList(
				mode = ListMode.KEYED,
				key = "id",
				presence = ListPresence.DECLARED_ONLY,
				unknownEntries = ListUnknownEntries.ALLOW
		)
		public List<ProviderEntry> providers = new ArrayList<>();
	}

	static final class NestedProviderDocument {
		public NestedProvider provider;
	}

	static final class NestedProvider {
		public String id;

		@ExtendableDocument
		public Verification verification;
	}

	static final class SingleCommandDefaults implements DefaultsProvider<SingleCommandDocument> {
		@Override
		public SingleCommandDocument supply(SingleCommandDocument document) {
			document.command = new CommandDefinition();
			return document;
		}
	}

	static final class CommandMapDefaults implements DefaultsProvider<CommandMapDocument> {
		@Override
		public CommandMapDocument supply(CommandMapDocument document) {
			document.commands.put("login", new CommandDefinition());
			document.commands.put("info", new CommandDefinition());
			return document;
		}
	}

	static final class ProviderListDefaults implements DefaultsProvider<ProviderListDocument> {
		@Override
		public ProviderListDocument supply(ProviderListDocument document) {
			ProviderEntry corporate = new ProviderEntry();
			corporate.id = "corporate";
			document.providers.add(corporate);
			return document;
		}
	}

	static final class NestedDefaults implements DefaultsProvider<NestedProviderDocument> {
		@Override
		public NestedProviderDocument supply(NestedProviderDocument document) {
			document.provider = new NestedProvider();
			return document;
		}
	}

	@ExtendableDocument
	static class CommandDefinition {
		public boolean enabled;
		public String description;
	}

	static final class SecureCommandDefinition extends CommandDefinition {
		public String inputMode;
	}

	static final class CommandDefaults implements DefaultsProvider<CommandDefinition> {
		@Override
		public CommandDefinition supply(CommandDefinition command) {
			command.enabled = true;
			return command;
		}
	}

	static final class SecureCommandDefaults implements DefaultsProvider<SecureCommandDefinition> {
		@Override
		public SecureCommandDefinition supply(SecureCommandDefinition command) {
			command.inputMode = "PASSWORD";
			return command;
		}
	}

	@ExtendableDocument
	@PreserveUnknownFields
	static class ProviderEntry {
		public String id;
	}

	static final class CorporateProviderEntry extends ProviderEntry {
		public List<String> domainWhitelist = new ArrayList<>();
	}

	static final class CorporateProviderDefaults implements DefaultsProvider<CorporateProviderEntry> {
		@Override
		public CorporateProviderEntry supply(CorporateProviderEntry entry) {
			entry.domainWhitelist = List.of("corp.example.com");
			return entry;
		}
	}

	static class Verification {
		public String type;
	}

	static final class SmsVerification extends Verification {
		public String template;
	}

	static final class VerificationDefaults implements DefaultsProvider<Verification> {
		@Override
		public Verification supply(Verification verification) {
			verification.type = "base";
			return verification;
		}
	}

	static final class SmsVerificationDefaults implements DefaultsProvider<SmsVerification> {
		@Override
		public SmsVerification supply(SmsVerification verification) {
			verification.template = "sms-default";
			return verification;
		}
	}
}
