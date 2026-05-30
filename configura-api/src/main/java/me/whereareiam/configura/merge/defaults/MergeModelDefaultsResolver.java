package me.whereareiam.configura.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.document.DocumentTypeContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves registered model defaults for a type during merge or defaults expansion.
 */
public interface MergeModelDefaultsResolver {
	/**
	 * Resolves defaults for the given model type.
	 *
	 * @param type model type
	 * @return resolved defaults node, or {@code null} when no registered defaults exist
	 */
	@Nullable JsonNode resolve(@NotNull Class<?> type);

	/**
	 * Resolves defaults for the given model type in the current document context.
	 *
	 * @param type model type
	 * @param context current document context
	 * @return resolved defaults node, or {@code null} when no registered defaults exist
	 */
	default @Nullable JsonNode resolve(@NotNull Class<?> type, @Nullable DocumentTypeContext context) {
		return resolve(type);
	}
}
