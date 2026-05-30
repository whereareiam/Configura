package me.whereareiam.configura.document;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Structural context supplied to generic document runtime components during type resolution.
 */
public final class DocumentTypeContext {
	private final @Nullable JsonNode currentNode;
	private final @Nullable JsonNode parentNode;
	private final @Nullable Field field;
	private final @Nullable String fieldName;
	private final @Nullable String mapKey;
	private final @Nullable String keyedListEntryKey;

	/**
	 * Creates a document type context.
	 *
	 * @param currentNode current node being resolved
	 * @param parentNode parent node when available
	 * @param field containing field when available
	 * @param fieldName serialized field name when available
	 * @param mapKey map key when available
	 * @param keyedListEntryKey keyed-list entry key when available
	 */
	public DocumentTypeContext(
			@Nullable JsonNode currentNode,
			@Nullable JsonNode parentNode,
			@Nullable Field field,
			@Nullable String fieldName,
			@Nullable String mapKey,
			@Nullable String keyedListEntryKey
	) {
		this.currentNode = currentNode;
		this.parentNode = parentNode;
		this.field = field;
		this.fieldName = fieldName;
		this.mapKey = mapKey;
		this.keyedListEntryKey = keyedListEntryKey;
	}

	public @Nullable JsonNode getCurrentNode() {
		return currentNode;
	}

	public @Nullable JsonNode getParentNode() {
		return parentNode;
	}

	public @Nullable Field getField() {
		return field;
	}

	public @Nullable String getFieldName() {
		return fieldName;
	}

	public @Nullable String getMapKey() {
		return mapKey;
	}

	public @Nullable String getKeyedListEntryKey() {
		return keyedListEntryKey;
	}
}
