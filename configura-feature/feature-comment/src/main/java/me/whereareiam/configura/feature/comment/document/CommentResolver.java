package me.whereareiam.configura.feature.comment.document;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.document.DocumentProcessor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.feature.comment.api.annotation.Comment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the {@link Comment}s of a model for the keys a document actually has, following the
 * document into sections, map values and list entries.
 */
@RequiredArgsConstructor
public final class CommentResolver {
	private final DocumentProcessor documents;

	/**
	 * Resolves the comments of a document bound to the given type.
	 *
	 * @param document the document as it will be written
	 * @param type     the model type of the document
	 * @return the comments, empty when the model declares none for the document's keys
	 */
	public @NotNull DocumentComments resolve(@Nullable JsonNode document, @Nullable Class<?> type) {
		if (document == null || type == null) return DocumentComments.none();

		Map<String, List<String>> lines = new HashMap<>();
		Class<?> rootType = documents.resolveType(type, new DocumentTypeContext(document, null, null, null, null, null));
		Comment header = rootType.getAnnotation(Comment.class);
		if (header != null) lines.put("", List.of(header.value()));

		object(document, rootType, new ArrayList<>(), lines);

		return DocumentComments.of(lines);
	}

	private void object(JsonNode node, Class<?> type, List<String> path, Map<String, List<String>> lines) {
		if (!node.isObject() || isOpaque(type)) return;

		node.properties().forEach(property -> {
			Field field = SerializedFieldResolver.resolveField(type, property.getKey());
			if (field == null) return;

			path.add(property.getKey());
			Comment comment = field.getAnnotation(Comment.class);
			if (comment != null) lines.put(DocumentComments.key(path), List.of(comment.value()));

			value(property.getValue(), node, field, property.getKey(), type, path, lines);
			path.remove(path.size() - 1);
		});
	}

	private void value(
			JsonNode value,
			JsonNode parent,
			Field field,
			String key,
			Class<?> ownerType,
			List<String> path,
			Map<String, List<String>> lines
	) {
		Class<?> declared = SerializedFieldResolver.valueType(ownerType, field);
		if (Map.class.isAssignableFrom(field.getType()) && value.isObject()) {
			mapEntries(value, field, declared, path, lines);
			return;
		}

		if (List.class.isAssignableFrom(field.getType()) && value.isArray()) {
			listEntries(value, field, declared, path, lines);
			return;
		}

		DocumentTypeContext context = new DocumentTypeContext(value, parent, field, key, null, null);
		object(value, documents.resolveType(declared, context), path, lines);
	}

	private void mapEntries(JsonNode map, Field field, Class<?> declared, List<String> path, Map<String, List<String>> lines) {
		map.properties().forEach(entry -> {
			DocumentTypeContext context = new DocumentTypeContext(entry.getValue(), null, field, field.getName(), entry.getKey(), null);

			path.add(entry.getKey());
			object(entry.getValue(), documents.resolveType(declared, context), path, lines);
			path.remove(path.size() - 1);
		});
	}

	private void listEntries(JsonNode list, Field field, Class<?> declared, List<String> path, Map<String, List<String>> lines) {
		for (int index = 0; index < list.size(); index++) {
			JsonNode entry = list.get(index);
			DocumentTypeContext context = new DocumentTypeContext(entry, null, field, field.getName(), null, null);

			path.add(String.valueOf(index));
			object(entry, documents.resolveType(declared, context), path, lines);
			path.remove(path.size() - 1);
		}
	}

	/** Types whose fields are not settings of the document: the JDK's own and Jackson trees. */
	private static boolean isOpaque(Class<?> type) {
		return type.isPrimitive()
				|| type.isEnum()
				|| type.isArray()
				|| type.getName().startsWith("java.")
				|| JsonNode.class.isAssignableFrom(type);
	}
}
