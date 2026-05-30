package me.whereareiam.configura.document;

import org.jetbrains.annotations.NotNull;

/**
 * Document processing phase invoked after a document has been created and populated.
 */
public interface DocumentPhase {
	/**
	 * Called after the document has been created and populated.
	 *
	 * @param value processed document instance
	 */
	void afterBind(@NotNull Object value);
}
