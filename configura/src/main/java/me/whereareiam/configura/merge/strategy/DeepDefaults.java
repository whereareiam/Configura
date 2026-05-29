package me.whereareiam.configura.merge.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Recursively fills source fields from defaults when they are missing.
 */
public final class DeepDefaults implements FieldMergeStrategy {
	@Override
	public @Nullable JsonNode merge(@NotNull MergeContext context) {
		JsonNode source = context.getSourceNode();
		JsonNode defaults = context.getDefaultNode();
		if (source == null || source.isNull() || context.sourceTreatsDefaultAsMissing())
			return defaults == null ? null : defaults.deepCopy();
		if (source.isObject() && defaults != null && defaults.isObject())
			return context.mergeChildren(source, defaults);

		return source.deepCopy();
	}
}
