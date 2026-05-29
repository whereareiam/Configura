package me.whereareiam.configura.merge.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps source data and never applies defaults.
 */
public final class NeverDefaults implements FieldMergeStrategy {
	@Override
	public @Nullable JsonNode merge(@NotNull MergeContext context) {
		JsonNode source = context.getSourceNode();
		return source == null
				? null
				: source.deepCopy();
	}
}
