package me.whereareiam.configura.merge.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies a default node to a source node for one merge field.
 *
 * <p>Strategies are selected through {@code @Merge(SomeStrategy.class)}, configured as the default
 * with {@code Config.builder().defaultMergeStrategy(...)}, or registered by name with
 * {@code Config.builder().mergeStrategy(...)}.</p>
 */
public interface MergeStrategy {
	/**
	 * Merges one field value.
	 *
	 * @param context merge inputs and recursive helpers
	 * @return node to write for the field, or {@code null} to omit it
	 */
	@Nullable JsonNode merge(@NotNull MergeContext context);
}
