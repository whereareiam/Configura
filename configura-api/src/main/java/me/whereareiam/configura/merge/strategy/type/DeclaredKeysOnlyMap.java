package me.whereareiam.configura.merge.strategy.type;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.strategy.MergeStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies defaults only inside map keys already declared by the source.
 */
public final class DeclaredKeysOnlyMap implements MergeStrategy {
	@Override
	public @Nullable JsonNode merge(@NotNull MergeContext context) {
		JsonNode source = context.getSourceNode();
		JsonNode defaults = context.getDefaultNode();
		if (source == null || source.isNull() || context.sourceTreatsDefaultAsMissing())
			return defaults == null ? null : defaults.deepCopy();
		if (source.isObject() && defaults != null && defaults.isObject())
			return context.mergeDeclaredChildren(source, defaults);

		return source.deepCopy();
	}
}
