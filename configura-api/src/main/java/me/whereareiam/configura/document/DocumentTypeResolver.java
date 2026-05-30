package me.whereareiam.configura.document;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves an effective document type for a declared type in a structural context.
 */
public interface DocumentTypeResolver {
	/**
	 * Resolves a more specific document type for the declared type.
	 *
	 * @param declaredType declared document type
	 * @param context structural document context
	 * @return resolved type, or {@code null} when this resolver does not apply
	 */
	@Nullable Class<?> resolve(@NotNull Class<?> declaredType, @NotNull DocumentTypeContext context);
}
