package me.whereareiam.configura.merge.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies defaults only when the source field is missing.
 */
public final class SourceOwnsField implements FieldMergeStrategy {
	@Override
	public @Nullable JsonNode merge(@NotNull MergeContext context) {
		JsonNode source = context.getSourceNode();
		if (source != null && !source.isNull()) return source.deepCopy();

		JsonNode defaults = context.getDefaultNode();
		return defaults == null
				? null
				: defaults.deepCopy();
	}
}
