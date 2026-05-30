package me.whereareiam.configura.feature.extension;

import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.document.DocumentTypeResolver;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.feature.extension.api.ConfigDocumentRule;
import me.whereareiam.configura.feature.extension.api.ConfigDocumentRuleRegistry;
import me.whereareiam.configura.feature.extension.api.annotation.ExtendableDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class ExtensionRuleTypeResolver implements DocumentTypeResolver {
	private final ConfigDocumentRuleRegistry registry;

	public ExtensionRuleTypeResolver(ConfigDocumentRuleRegistry registry) {
		this.registry = registry != null ? registry.copy() : new ConfigDocumentRuleRegistry();
	}

	public boolean hasRules(@NotNull Class<?> declaredType) {
		return registry.hasRules(declaredType);
	}

	@Override
	public @Nullable Class<?> resolve(
			@NotNull Class<?> declaredType,
			@NotNull DocumentTypeContext context
	) {
		if (!registry.hasRules(declaredType)) return null;
		if (!isExtendable(declaredType, context.getField()))
			throw new ConfigException("Document type " + declaredType.getName() + " is not extendable in this location");

		List<ConfigDocumentRule<?>> matches = new ArrayList<>();
		for (ConfigDocumentRule<?> rule : registry.getContextualRules(declaredType)) {
			if (rule.getSelector().test(context))
				matches.add(rule);
		}
		if (matches.size() > 1) throw new ConfigException("Multiple contextual document rules matched " + declaredType.getName());
		if (matches.size() == 1) return matches.getFirst().getTargetType();

		ConfigDocumentRule<?> wholeRule = registry.getWholeRule(declaredType);
		return wholeRule != null ? wholeRule.getTargetType() : null;
	}

	private boolean isExtendable(@NotNull Class<?> declaredType, @Nullable Field containingField) {
		return declaredType.isAnnotationPresent(ExtendableDocument.class)
				|| (containingField != null && containingField.isAnnotationPresent(ExtendableDocument.class));
	}
}
