package me.whereareiam.configura.feature.extension;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.ConfiguraFeature;
import me.whereareiam.configura.document.DocumentTypeResolver;
import me.whereareiam.configura.feature.extension.api.ConfigDocumentRule;
import me.whereareiam.configura.feature.extension.api.ConfigDocumentRuleRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ExtensionFeature implements ConfiguraFeature {
	private final ConfigDocumentRuleRegistry rules = new ConfigDocumentRuleRegistry();

	public static @NotNull ExtensionFeature defaults() {
		return new ExtensionFeature();
	}

	public static @NotNull ExtensionFeature rule(@NotNull ConfigDocumentRule<?> rule) {
		return defaults().addRule(rule);
	}

	public @NotNull ExtensionFeature addRule(@NotNull ConfigDocumentRule<?> rule) {
		rules.register(rule);
		return this;
	}

	public @NotNull ConfigDocumentRuleRegistry rules() {
		return rules.copy();
	}

	@Override
	public @NotNull List<DocumentTypeResolver> typeResolvers() {
		return List.of(new ExtensionRuleTypeResolver(rules));
	}

	@Override
	public @NotNull List<Module> modules(@NotNull ObjectMapper plainMapper) {
		return List.of(new ExtensionFeatureModule(new ExtensionRuleTypeResolver(rules), plainMapper));
	}
}
