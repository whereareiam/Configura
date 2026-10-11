package me.whereareiam.configura.feature.comment.document;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The comment lines of one document, keyed by the path of the key they are written above. The
 * empty path holds the header of the document.
 */
public final class DocumentComments {
	private static final DocumentComments NONE = new DocumentComments(Map.of());

	private final Map<String, List<String>> lines;

	private DocumentComments(Map<String, List<String>> lines) {
		this.lines = lines;
	}

	/**
	 * Returns the comments of a document that has none.
	 *
	 * @return empty comments
	 */
	public static @NotNull DocumentComments none() {
		return NONE;
	}

	static @NotNull DocumentComments of(Map<String, List<String>> lines) {
		return lines.isEmpty() ? NONE : new DocumentComments(new HashMap<>(lines));
	}

	/**
	 * Returns whether the document has no comment at all.
	 *
	 * @return {@code true} without comments
	 */
	public boolean isEmpty() {
		return lines.isEmpty();
	}

	/**
	 * Returns the lines written above the key at the given path, or an empty list.
	 *
	 * @param path keys from the root to the key, list positions as numbers
	 * @return the comment lines
	 */
	public @NotNull List<String> at(@NotNull List<String> path) {
		return lines.getOrDefault(key(path), List.of());
	}

	static String key(List<String> path) {
		StringBuilder key = new StringBuilder();
		for (String segment : path)
			key.append('/').append(segment.replace("~", "~0").replace("/", "~1"));

		return key.toString();
	}
}
