package me.whereareiam.configura.document;

import com.fasterxml.jackson.core.JsonGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Takes part in writing a document by choosing the generator the document is serialized through.
 *
 * <p>Hooks form a chain in the order their features are registered. A hook usually asks the rest of
 * the chain for the generator and returns it, wrapped if it wants to see or add to what is written.
 * It may instead open a generator of its own on the output of the {@link DocumentWrite}; hooks after it are then
 * not asked. The end of the chain opens the generator of the instance's mapper. Configura closes the
 * returned generator after writing.</p>
 *
 * <pre>{@code
 * public final class MarkerHook implements DocumentWriteHook {
 * 	@Override
 * 	public @NotNull JsonGenerator open(@NotNull DocumentWrite write, @NotNull Next next) throws IOException {
 * 		JsonGenerator generator = next.open();
 * 		return write.getModelType() == null ? generator : new MarkingGenerator(generator);
 * 	}
 * }
 * }</pre>
 *
 * <p>Without any hook, Configura writes documents exactly as before hooks existed.</p>
 */
@FunctionalInterface
public interface DocumentWriteHook {
	/**
	 * Returns the generator to write the document through.
	 *
	 * @param write the document being written
	 * @param next  opens the generator of the rest of the chain
	 * @return the generator to write through
	 * @throws IOException if a generator cannot be opened
	 */
	@NotNull JsonGenerator open(@NotNull DocumentWrite write, @NotNull Next next) throws IOException;

	/**
	 * The rest of the hook chain.
	 */
	@FunctionalInterface
	interface Next {
		/**
		 * Opens the generator the rest of the chain chooses.
		 *
		 * @return the generator
		 * @throws IOException if a generator cannot be opened
		 */
		@NotNull JsonGenerator open() throws IOException;
	}
}
