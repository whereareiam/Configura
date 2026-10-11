package me.whereareiam.configura.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

/**
 * One document being written, as a {@link DocumentWriteHook} sees it.
 */
@Getter
@RequiredArgsConstructor
public final class DocumentWrite {
	/**
	 * The content that is written.
	 */
	private final @NotNull JsonNode document;

	/**
	 * The model of the document: the type given to {@code update}, the class of the value given to
	 * {@code save}, {@code write} or {@code writeBytes}, or {@code null} for {@code writeNode}.
	 */
	private final @Nullable Class<?> modelType;

	/**
	 * The mapper of the Configura instance.
	 */
	private final @NotNull ObjectMapper mapper;

	/**
	 * The stream the document is written to. A hook that opens its own generator opens it here.
	 */
	private final @NotNull OutputStream output;

	/**
	 * Resolves which type a section of the document has, including types other features pick.
	 */
	private final @NotNull DocumentProcessor documents;
}
