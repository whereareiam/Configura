package me.whereareiam.configura.merge.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Materializes an object when missing, while keeping declared object children source-owned.
 */
public final class StructuralObject implements FieldMergeStrategy {
	@Override
	public @Nullable JsonNode merge(@NotNull MergeContext context) {
		JsonNode source = context.getSourceNode();
		if (source != null && !source.isNull() && !context.sourceTreatsDefaultAsMissing())
			return source.deepCopy();

		JsonNode defaults = context.getDefaultNode();
		return defaults == null ? null : defaults.deepCopy();
	}
}
