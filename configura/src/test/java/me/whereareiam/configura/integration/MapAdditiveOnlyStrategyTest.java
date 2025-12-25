package me.whereareiam.configura.integration;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.annotation.MergeStrategy;
import me.whereareiam.configura.annotation.Policy;
import me.whereareiam.configura.annotation.Template;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link MergeStrategy#MAP_ADDITIVE_ONLY} policy.
 *
 * <p>Tests the key behavior: template Map entries should only be added if the field
 * itself is missing from user config. Once user has the field, no keys should be
 * added from templates, allowing permanent deletion of unwanted entries.</p>
 */
public class MapAdditiveOnlyStrategyTest {

	/**
	 * Simulates a language configuration like Yui's Languages config.
	 */
	@Template(supplier = @Template.Supplier(LanguagesConfigTemplate.class))
	public static class LanguagesConfig {
		@Field
		@Policy(MergeStrategy.MAP_ADDITIVE_ONLY)
		public Map<String, LanguageEntry> languages;
	}

	public static class LanguageEntry {
		@Field
		public boolean enabled;
		@Field
		public String displayName;
	}

	public static class LanguagesConfigTemplate implements TemplateProvider<LanguagesConfig> {
		@Override
		public LanguagesConfig supply(LanguagesConfig base) {
			if (base.languages == null) {
				base.languages = new LinkedHashMap<>();
			}

			// Template provides en-US and de by default
			if (!base.languages.containsKey("en-US")) {
				LanguageEntry enUS = new LanguageEntry();
				enUS.enabled = true;
				enUS.displayName = "English (US)";
				base.languages.put("en-US", enUS);
			}

			if (!base.languages.containsKey("de")) {
				LanguageEntry de = new LanguageEntry();
				de.enabled = false;
				de.displayName = "German";
				base.languages.put("de", de);
			}

			return base;
		}
	}

	/**
	 * Simulates a command configuration with nested requirements.
	 */
	@Template(supplier = @Template.Supplier(CommandsConfigTemplate.class))
	public static class CommandsConfig {
		@Field
		@Policy(MergeStrategy.MAP_ADDITIVE_ONLY)
		public Map<String, CommandDefinition> commands;
	}

	public static class CommandDefinition {
		@Field
		public boolean enabled;
		@Field
		public String description;
		@Field
		@Policy(MergeStrategy.MAP_ADDITIVE_ONLY)
		public Requirements requirements;
	}

	public static class Requirements {
		@Field
		public String operator;
		@Field
		@Policy(MergeStrategy.MAP_ADDITIVE_ONLY)
		public Map<String, RequirementEntry> groups;
	}

	public static class RequirementEntry {
		@Field
		public String type;
		@Field
		public String value;
	}

	public static class CommandsConfigTemplate implements TemplateProvider<CommandsConfig> {
		@Override
		public CommandsConfig supply(CommandsConfig base) {
			if (base.commands == null) {
				base.commands = new LinkedHashMap<>();
			}

			// Template provides reload command with requirements
			if (!base.commands.containsKey("reload")) {
				CommandDefinition reload = new CommandDefinition();
				reload.enabled = true;
				reload.description = "Reload configuration";

				reload.requirements = new Requirements();
				reload.requirements.operator = "AND";
				reload.requirements.groups = new LinkedHashMap<>();

				RequirementEntry permission = new RequirementEntry();
				permission.type = "PERMISSION";
				permission.value = "admin.reload";
				reload.requirements.groups.put("permission", permission);

				base.commands.put("reload", reload);
			}

			// Template provides help command without requirements
			if (!base.commands.containsKey("help")) {
				CommandDefinition help = new CommandDefinition();
				help.enabled = true;
				help.description = "Show help";
				help.requirements = null;
				base.commands.put("help", help);
			}

			return base;
		}
	}

	@Test
	void firstCreation_appliesTemplateDefaults(@TempDir Path dir) {
		Path file = dir.resolve("languages.yml");

		// First update with no existing file - should apply template defaults
		LanguagesConfig loaded = Config.update(file, LanguagesConfig.class);

		// Should have template defaults
		assertNotNull(loaded.languages);
		assertEquals(2, loaded.languages.size());
		assertTrue(loaded.languages.containsKey("en-US"));
		assertTrue(loaded.languages.containsKey("de"));
		assertEquals("English (US)", loaded.languages.get("en-US").displayName);
	}

	@Test
	void userDeletesEntry_entryStaysDeleted(@TempDir Path dir) {
		Path file = dir.resolve("languages.yml");

		// First update creates defaults
		LanguagesConfig initial = Config.update(file, LanguagesConfig.class);

		// Delete German language
		initial.languages.remove("de");
		assertEquals(1, initial.languages.size());

		// Update again (simulating next run)
		Config.save(file, initial);
		LanguagesConfig afterDeletion = Config.update(file, LanguagesConfig.class);

		// CRITICAL: German should NOT reappear from template
		assertEquals(1, afterDeletion.languages.size());
		assertTrue(afterDeletion.languages.containsKey("en-US"));
		assertFalse(afterDeletion.languages.containsKey("de"),
			"Deleted language entry should NOT reappear from template");
	}

	@Test
	void userDeletesAllEntries_emptyMapPersists(@TempDir Path dir) {
		Path file = dir.resolve("languages.yml");

		// First update creates defaults
		LanguagesConfig initial = Config.update(file, LanguagesConfig.class);

		// Clear all languages
		initial.languages.clear();

		// Update again
		Config.save(file, initial);
		LanguagesConfig afterClear = Config.update(file, LanguagesConfig.class);

		// Empty map should persist, no template entries added
		assertNotNull(afterClear.languages);
		assertEquals(0, afterClear.languages.size(),
			"Empty map should persist without template entries being re-added");
	}

	@Test
	void userAddsCustomEntry_customEntryPersists(@TempDir Path dir) {
		Path file = dir.resolve("languages.yml");

		// First update creates defaults
		LanguagesConfig initial = Config.update(file, LanguagesConfig.class);

		// Delete defaults, add custom
		initial.languages.clear();

		LanguageEntry french = new LanguageEntry();
		french.enabled = true;
		french.displayName = "French";
		initial.languages.put("fr", french);

		// Update again
		Config.save(file, initial);
		LanguagesConfig afterCustom = Config.update(file, LanguagesConfig.class);

		// Only custom entry should exist
		assertEquals(1, afterCustom.languages.size());
		assertTrue(afterCustom.languages.containsKey("fr"));
		assertFalse(afterCustom.languages.containsKey("en-US"));
		assertFalse(afterCustom.languages.containsKey("de"));
	}

	@Test
	void multipleUpdateCycles_deletionsPersist(@TempDir Path dir) {
		Path file = dir.resolve("languages.yml");

		// Cycle 1: Create with defaults
		LanguagesConfig cycle1 = Config.update(file, LanguagesConfig.class);
		assertEquals(2, cycle1.languages.size());

		// Cycle 2: Delete one entry and update
		cycle1.languages.remove("de");
		Config.save(file, cycle1);
		LanguagesConfig cycle2 = Config.update(file, LanguagesConfig.class);
		assertEquals(1, cycle2.languages.size());

		// Cycle 3: Another update cycle without changes
		LanguagesConfig cycle3 = Config.update(file, LanguagesConfig.class);
		assertEquals(1, cycle3.languages.size());

		// Cycle 4: Modify value and update
		cycle3.languages.get("en-US").displayName = "American English";
		Config.save(file, cycle3);
		LanguagesConfig cycle4 = Config.update(file, LanguagesConfig.class);
		assertEquals(1, cycle4.languages.size());
		assertFalse(cycle4.languages.containsKey("de"),
			"Deleted entry should not reappear after multiple update cycles");
	}

	@Test
	void nestedMapAdditiveOnly_userDeletesRequirement(@TempDir Path dir) {
		Path file = dir.resolve("commands.yml");

		// First update creates defaults (reload with permission requirement, help without)
		CommandsConfig loaded = Config.update(file, CommandsConfig.class);

		// Verify defaults
		assertEquals(2, loaded.commands.size());
		assertNotNull(loaded.commands.get("reload").requirements);
		assertEquals(1, loaded.commands.get("reload").requirements.groups.size());
		assertNull(loaded.commands.get("help").requirements);

		// User deletes the permission requirement from reload command
		loaded.commands.get("reload").requirements.groups.remove("permission");

		// Update again
		Config.save(file, loaded);
		CommandsConfig afterDeletion = Config.update(file, CommandsConfig.class);

		// Requirement should stay deleted
		assertEquals(0, afterDeletion.commands.get("reload").requirements.groups.size(),
			"Deleted requirement group should NOT reappear from template");
	}

	@Test
	void nestedMapAdditiveOnly_userDeletesEntireRequirementsBlock(@TempDir Path dir) {
		Path file = dir.resolve("commands.yml");

		// First update creates defaults
		CommandsConfig loaded = Config.update(file, CommandsConfig.class);

		// User removes entire requirements block from reload command
		loaded.commands.get("reload").requirements = null;

		// Update again
		Config.save(file, loaded);
		CommandsConfig afterDeletion = Config.update(file, CommandsConfig.class);

		// Requirements block should stay null
		assertNull(afterDeletion.commands.get("reload").requirements,
			"Deleted requirements block should NOT reappear from template");
	}

	@Test
	void nestedMapAdditiveOnly_userDeletesEntireCommand(@TempDir Path dir) {
		Path file = dir.resolve("commands.yml");

		// First update creates defaults
		CommandsConfig loaded = Config.update(file, CommandsConfig.class);

		// User deletes reload command entirely
		loaded.commands.remove("reload");

		// Update again
		Config.save(file, loaded);
		CommandsConfig afterDeletion = Config.update(file, CommandsConfig.class);

		// Command should stay deleted
		assertEquals(1, afterDeletion.commands.size());
		assertFalse(afterDeletion.commands.containsKey("reload"),
			"Deleted command should NOT reappear from template");
		assertTrue(afterDeletion.commands.containsKey("help"));
	}

	// Models for DEFAULT strategy test
	@Template(supplier = @Template.Supplier(DefaultStrategyTemplate.class))
	public static class DefaultStrategyConfig {
		@Field
		@Policy // Defaults to MergeStrategy.DEFAULT
		public Map<String, SettingsGroup> settings;
	}

	public static class SettingsGroup {
		@Field
		public int timeout;
		@Field
		public boolean enabled;
	}

	public static class DefaultStrategyTemplate implements TemplateProvider<DefaultStrategyConfig> {
		@Override
		public DefaultStrategyConfig supply(DefaultStrategyConfig base) {
			if (base.settings == null) {
				base.settings = new HashMap<>();
			}

			if (!base.settings.containsKey("group1")) {
				SettingsGroup g1 = new SettingsGroup();
				g1.timeout = 5000;
				g1.enabled = true;
				base.settings.put("group1", g1);
			}

			return base;
		}
	}

	// Models for SKIP strategy test
	@Template(supplier = @Template.Supplier(SkipStrategyTemplate.class))
	public static class SkipStrategyConfig {
		@Field
		@Policy(MergeStrategy.SKIP)
		public Map<String, String> userControlled;
	}

	public static class SkipStrategyTemplate implements TemplateProvider<SkipStrategyConfig> {
		@Override
		public SkipStrategyConfig supply(SkipStrategyConfig base) {
			if (base.userControlled == null) {
				base.userControlled = new HashMap<>();
			}
			base.userControlled.put("shouldNotAppear", "value");
			return base;
		}
	}

	@Test
	void defaultStrategy_stillWorks_deepMerge(@TempDir Path dir) {
		Path file = dir.resolve("default-strategy.yml");

		// First update creates defaults
		DefaultStrategyConfig loaded = Config.update(file, DefaultStrategyConfig.class);

		// Delete entry
		loaded.settings.remove("group1");

		// Update again
		Config.save(file, loaded);
		DefaultStrategyConfig afterDeletion = Config.update(file, DefaultStrategyConfig.class);

		// With DEFAULT strategy, template entries SHOULD reappear
		assertTrue(afterDeletion.settings.containsKey("group1"),
			"With DEFAULT merge strategy, deleted entries should reappear from template");
	}

	@Test
	void skipStrategy_noTemplateApplication(@TempDir Path dir) {
		Path file = dir.resolve("skip-strategy.yml");

		// First update - with SKIP strategy, template should NOT be applied
		SkipStrategyConfig loaded = Config.update(file, SkipStrategyConfig.class);

		assertNull(loaded.userControlled,
			"With SKIP strategy, template should not be applied");
	}

	@Test
	void fileManuallyEditedWithDeletion_persistsAcrossUpdate(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("languages.yml");

		// Create initial file with template defaults
		LanguagesConfig initial = Config.update(file, LanguagesConfig.class);
		assertEquals(2, initial.languages.size());

		// Manually edit file to remove German entry (simulating user editing YAML directly)
		String content = Files.readString(file);
		// Remove the de entry and its properties
		content = content.replaceAll("(?m)^\\s*de:.*$", "")
				.replaceAll("(?m)^\\s*enabled:\\s*false.*$", "")
				.replaceAll("(?m)^\\s*displayName:\\s*German.*$", "");
		Files.writeString(file, content);

		// Update operation (like Config.update() would do)
		LanguagesConfig updated = Config.update(file, LanguagesConfig.class);

		// Manually deleted entry should NOT reappear
		assertEquals(1, updated.languages.size());
		assertFalse(updated.languages.containsKey("de"),
			"Entry deleted by manual file edit should not reappear during update");
	}
}
