package me.whereareiam.configura.document;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Core document processor abstraction used to resolve effective document types and run document
 * phases without knowing which feature supplied them.
 */
public interface DocumentProcessor {
	/**
	 * Resolves the effective document type for the declared type and context.
	 *
	 * @param declaredType declared document type
	 * @param context structural context, or {@code null} for root binding/defaults
	 * @return effective document type
	 */
	@NotNull Class<?> resolveType(@NotNull Class<?> declaredType, @Nullable DocumentTypeContext context);

	/**
	 * Runs registered document phases after a document instance has been created.
	 *
	 * @param value processed document instance, or {@code null}
	 */
	void afterBind(@Nullable Object value);
}
