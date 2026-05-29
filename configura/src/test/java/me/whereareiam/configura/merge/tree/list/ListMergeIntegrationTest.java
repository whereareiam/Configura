package me.whereareiam.configura.merge.tree.list;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.annotation.Merge;
import me.whereareiam.configura.merge.annotation.MergeList;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.type.merge.tree.list.ListMode;
import me.whereareiam.configura.type.merge.tree.list.ListPresence;
import me.whereareiam.configura.type.merge.tree.list.ListUnknownEntries;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Config Collection Merge")
class ListMergeIntegrationTest {
	@Test
	@DisplayName("Missing keyed list fields seed the whole default list")
	void missingKeyedListFieldsSeedWholeDefaultList(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");

		ProviderListConfig config = yaml(ProviderListDefaults.class).update(file, ProviderListConfig.class);

		assertEquals(2, config.providers.size());
		assertEquals("premium", config.providers.get(0).id);
		assertEquals("credential", config.providers.get(1).id);
		assertTrue(Files.readString(file).contains("premium"));
		assertTrue(Files.readString(file).contains("credential"));
	}

	@Test
	@DisplayName("Explicit empty keyed list stays empty under declared only presence")
	void explicitEmptyKeyedListStaysEmptyUnderDeclaredOnly(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");
		Files.writeString(file, "providers: []\n");

		ProviderListConfig config = yaml(ProviderListDefaults.class).update(file, ProviderListConfig.class);

		assertTrue(config.providers.isEmpty());
		assertTrue(Files.readString(file).contains("providers: []"));
	}

	@Test
	@DisplayName("Matching keyed entries receive missing defaults")
	void matchingKeyedEntriesReceiveMissingDefaults(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");
		Files.writeString(file, """
				providers:
				  - id: premium
				    enabled: false
				""");

		ProviderListConfig config = yaml(ProviderListDefaults.class).update(file, ProviderListConfig.class);

		assertEquals(1, config.providers.size());
		ProviderEntry premium = config.providers.get(0);
		assertEquals("premium", premium.id);
		assertFalse(premium.enabled);
		assertEquals(100, premium.priority);
		assertEquals("", premium.displayName);
		assertNotNull(premium.joinRestriction);
		assertFalse(premium.joinRestriction.enabled);
		assertNotNull(premium.verification);
		assertTrue(premium.verification.enabled);
		assertNotNull(premium.session);
		assertNotNull(premium.session.recognition);
		assertFalse(premium.session.recognition.allowOnUntrustedIps);
		assertNull(premium.session.concurrencyPolicy);
	}

	@Test
	@DisplayName("Custom keyed entries receive generic entry defaults without a keyed default match")
	void customKeyedEntriesReceiveGenericEntryDefaults(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");
		Files.writeString(file, """
				providers:
				  - id: community-oauth
				""");

		ProviderListConfig config = yaml(ProviderListDefaults.class).update(file, ProviderListConfig.class);

		assertEquals(1, config.providers.size());
		ProviderEntry provider = config.providers.get(0);
		assertEquals("community-oauth", provider.id);
		assertEquals("", provider.displayName);
		assertFalse(provider.enabled);
		assertEquals(0, provider.priority);
		assertNotNull(provider.joinRestriction);
		assertFalse(provider.joinRestriction.enabled);
		assertNotNull(provider.verification);
		assertFalse(provider.verification.enabled);
		assertNotNull(provider.session);
		assertNotNull(provider.session.recognition);
		assertTrue(provider.session.recognition.signals.isEmpty());
		assertNull(provider.session.concurrencyPolicy);
	}

	@Test
	@DisplayName("Seed defaults presence appends omitted default entries")
	void seedDefaultsPresenceAppendsOmittedDefaultEntries(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");
		Files.writeString(file, """
				providers:
				  - id: premium
				    enabled: false
				""");

		SeededProviderListConfig config = yaml(SeededProviderListDefaults.class).update(file, SeededProviderListConfig.class);

		assertEquals(2, config.providers.size());
		assertEquals("premium", config.providers.get(0).id);
		assertEquals("credential", config.providers.get(1).id);
	}

	@Test
	@DisplayName("Default domain only rejects unknown keyed entries")
	void defaultDomainOnlyRejectsUnknownKeyedEntries(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");
		Files.writeString(file, """
				providers:
				  - id: custom-provider
				""");

		ConfigException exception = assertThrows(
				ConfigException.class,
				() -> yaml(LockedProviderListDefaults.class).update(file, LockedProviderListConfig.class)
		);
		assertTrue(exception.getMessage().contains("custom-provider"));
	}

	@Test
	@DisplayName("Source owns field short-circuits keyed list merge")
	void sourceOwnsFieldShortCircuitsKeyedListMerge(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");
		Files.writeString(file, """
				providers:
				  - id: premium
				    enabled: false
				""");

		SourceOwnedProviderListConfig config = yaml(SourceOwnedProviderListDefaults.class).update(file, SourceOwnedProviderListConfig.class);

		assertEquals(1, config.providers.size());
		ProviderEntry premium = config.providers.get(0);
		assertEquals("premium", premium.id);
		assertFalse(premium.enabled);
		assertEquals(0, premium.priority);
	}

	@Test
	@DisplayName("Nested keyed list entries merge by id")
	void nestedKeyedListEntriesMergeById(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");
		Files.writeString(file, """
				providers:
				  - id: premium
				    verification:
				      methods:
				        - id: totp
				          priority: 200
				""");

		ProviderListConfig config = yaml(ProviderListDefaults.class).update(file, ProviderListConfig.class);

		assertEquals(1, config.providers.size());
		List<MethodEntry> methods = config.providers.get(0).verification.methods;
		assertEquals(1, methods.size());
		assertEquals("totp", methods.get(0).id);
		assertTrue(methods.get(0).enabled);
		assertEquals(200, methods.get(0).priority);
	}

	@Test
	@DisplayName("Duplicate source keys fail clearly")
	void duplicateSourceKeysFailClearly(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");
		Files.writeString(file, """
				providers:
				  - id: premium
				  - id: premium
				""");

		ConfigException exception = assertThrows(
				ConfigException.class,
				() -> yaml(ProviderListDefaults.class).update(file, ProviderListConfig.class)
		);
		assertTrue(exception.getMessage().contains("duplicate"));
		assertTrue(exception.getMessage().contains("premium"));
	}

	@Test
	@DisplayName("Missing source keys fail clearly")
	void missingSourceKeysFailClearly(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("providers.yml");
		Files.writeString(file, """
				providers:
				  - enabled: true
				""");

		ConfigException exception = assertThrows(
				ConfigException.class,
				() -> yaml(ProviderListDefaults.class).update(file, ProviderListConfig.class)
		);
		assertTrue(exception.getMessage().contains("key field 'id'"));
	}

	@Test
	@DisplayName("Duplicate default keys fail clearly")
	void duplicateDefaultKeysFailClearly(@TempDir Path tempDir) {
		Path file = tempDir.resolve("providers.yml");

		ConfigException exception = assertThrows(
				ConfigException.class,
				() -> yaml(DuplicateProviderListDefaults.class).update(file, ProviderListConfig.class)
		);
		assertTrue(exception.getMessage().contains("duplicate"));
		assertTrue(exception.getMessage().contains("premium"));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private Configura yaml(Class<? extends MergeDefaultsProvider<?>> defaultsProvider) {
		return Config.builder()
				.format(Format.YAML)
				.defaults((Class) defaultsProvider)
				.build();
	}

	public static class ProviderListConfig {
		@Merge
		@MergeList(
				mode = ListMode.KEYED,
				key = "id",
				presence = ListPresence.DECLARED_ONLY,
				unknownEntries = ListUnknownEntries.ALLOW
		)
		public List<ProviderEntry> providers = new ArrayList<>();
	}

	public static class SeededProviderListConfig {
		@Merge
		@MergeList(
				mode = ListMode.KEYED,
				key = "id",
				presence = ListPresence.SEED_DEFAULTS,
				unknownEntries = ListUnknownEntries.ALLOW
		)
		public List<ProviderEntry> providers = new ArrayList<>();
	}

	public static class LockedProviderListConfig {
		@Merge
		@MergeList(
				mode = ListMode.KEYED,
				key = "id",
				presence = ListPresence.DEFAULT_DOMAIN_ONLY,
				unknownEntries = ListUnknownEntries.REJECT
		)
		public List<ProviderEntry> providers = new ArrayList<>();
	}

	public static class SourceOwnedProviderListConfig {
		@Merge(SourceOwnsField.class)
		@MergeList(
				mode = ListMode.KEYED,
				key = "id",
				presence = ListPresence.SEED_DEFAULTS,
				unknownEntries = ListUnknownEntries.ALLOW
		)
		public List<ProviderEntry> providers = new ArrayList<>();
	}

	public static class ProviderEntry {
		public String id;
		public String displayName = "";
		public boolean enabled;
		public int priority;
		public JoinRestriction joinRestriction = new JoinRestriction();
		public Verification verification = new Verification();
		public Session session = new Session();
	}

	public static class JoinRestriction {
		public boolean enabled;
	}

	public static class Verification {
		public boolean enabled;
		public boolean required;
		@Merge
		@MergeList(
				mode = ListMode.KEYED,
				key = "id",
				presence = ListPresence.DECLARED_ONLY,
				unknownEntries = ListUnknownEntries.ALLOW
		)
		public List<MethodEntry> methods = new ArrayList<>();
	}

	public static class MethodEntry {
		public String id;
		public boolean enabled;
		public int priority;
	}

	public static class Session {
		public String concurrencyPolicy;
		public Recognition recognition = new Recognition();
	}

	public static class Recognition {
		public List<String> signals = new ArrayList<>();
		public boolean allowOnUntrustedIps;
	}

	public static class ProviderListDefaults implements MergeDefaultsProvider<ProviderListConfig> {
		@Override
		public ProviderListConfig supply(ProviderListConfig config) {
			config.providers = defaultProviders();
			return config;
		}
	}

	public static class SeededProviderListDefaults implements MergeDefaultsProvider<SeededProviderListConfig> {
		@Override
		public SeededProviderListConfig supply(SeededProviderListConfig config) {
			config.providers = defaultProviders();
			return config;
		}
	}

	public static class LockedProviderListDefaults implements MergeDefaultsProvider<LockedProviderListConfig> {
		@Override
		public LockedProviderListConfig supply(LockedProviderListConfig config) {
			config.providers = defaultProviders();
			return config;
		}
	}

	public static class SourceOwnedProviderListDefaults implements MergeDefaultsProvider<SourceOwnedProviderListConfig> {
		@Override
		public SourceOwnedProviderListConfig supply(SourceOwnedProviderListConfig config) {
			config.providers = defaultProviders();
			return config;
		}
	}

	public static class DuplicateProviderListDefaults implements MergeDefaultsProvider<ProviderListConfig> {
		@Override
		public ProviderListConfig supply(ProviderListConfig config) {
			config.providers = List.of(provider("premium", true, 100), provider("premium", true, 90));
			return config;
		}
	}

	private static List<ProviderEntry> defaultProviders() {
		ProviderEntry premium = provider("premium", true, 100);
		premium.verification.enabled = true;
		premium.verification.methods = List.of(method("totp", true, 100));

		ProviderEntry credential = provider("credential", true, 50);
		credential.verification.enabled = true;
		credential.verification.methods = List.of(method("totp", true, 100));

		return List.of(premium, credential);
	}

	private static ProviderEntry provider(String id, boolean enabled, int priority) {
		ProviderEntry entry = new ProviderEntry();
		entry.id = id;
		entry.enabled = enabled;
		entry.priority = priority;
		return entry;
	}

	private static MethodEntry method(String id, boolean enabled, int priority) {
		MethodEntry entry = new MethodEntry();
		entry.id = id;
		entry.enabled = enabled;
		entry.priority = priority;
		return entry;
	}
}
