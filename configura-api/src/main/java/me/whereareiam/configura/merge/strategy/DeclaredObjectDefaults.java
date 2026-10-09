package me.whereareiam.configura.merge.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps an object field absent until it is declared in the source, then deep-fills missing children.
 */
public final class DeclaredObjectDefaults implements FieldMergeStrategy {
	@Override
	public @Nullable JsonNode merge(@NotNull MergeContext context) {
		JsonNode source = context.getSourceNode();
		if (source == null || source.isNull() || context.sourceTreatsDefaultAsMissing())
			return null;

		JsonNode defaults = context.getDefaultNode();
		if (source.isObject() && defaults != null && defaults.isObject())
			return context.mergeChildren(source, defaults);

		return source.deepCopy();
	}
}
