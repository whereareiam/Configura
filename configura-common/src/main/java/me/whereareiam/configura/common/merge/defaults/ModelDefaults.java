package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.document.DocumentProcessor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds the document a model stands for when nobody has written a file yet.
 * <p>
 * The defaults of a type are what its {@link DefaultsProvider} returns for a freshly constructed
 * instance, or that instance itself when the type and its superclasses have no provider. An object
 * field left null takes the defaults of its own type, so a section does not need an initializer to
 * have defaults.
 */
@RequiredArgsConstructor
public final class ModelDefaults {
	private final ObjectMapper mapper;
	private final DefaultsProviderRegistry providers;
	private final DocumentProcessor documents;

	/**
	 * Returns the defaults of a model instance: its own values, completed by providers and by the
	 * defaults of its sections.
	 */
	public @NotNull ObjectNode of(@NotNull Object model, @NotNull Class<?> type, @NotNull PrimitiveDefaultPolicy policy) {
		Class<?> effectiveType = documents.resolveType(type, null);
		ObjectNode node = mapper.valueToTree(model) instanceof ObjectNode object ? object : mapper.createObjectNode();
		if (provided(effectiveType) instanceof ObjectNode provided) fill(node, provided, effectiveType, policy, null);

		completeSections(node, effectiveType, policy);

		return node;
	}

	/**
	 * Returns the defaults of a model type, or null when the type is not a model or has none.
	 */
	public @Nullable ObjectNode ofType(
			@NotNull Class<?> type,
			@NotNull PrimitiveDefaultPolicy policy,
			@Nullable DocumentTypeContext context
	) {
		Class<?> effectiveType = documents.resolveType(type, context);
		if (!isModel(effectiveType) || !hasDefaults(effectiveType)) return null;

		ObjectNode node = provided(effectiveType) instanceof ObjectNode provided ? provided : constructed(effectiveType);
		removeNulls(node);
		completeSections(node, effectiveType, policy);

		return node.isEmpty() ? null : node;
	}

	/** Gives every section its type's defaults: all of them when it is null, what it lacks when it is set. */
	private void completeSections(ObjectNode node, Class<?> type, PrimitiveDefaultPolicy policy) {
		for (Field field : SerializedFieldResolver.fields(type)) {
			if (!isSection(field)) continue;

			String key = SerializedFieldResolver.resolveSerializedName(field);
			JsonNode existing = node.get(key);
			DocumentTypeContext context = new DocumentTypeContext(existing, node, field, key, null, null);
			Class<?> sectionType = documents.resolveType(field.getType(), context);
			ObjectNode section = ofType(sectionType, policy, context);
			if (section == null) continue;

			if (isMissing(existing, policy)) node.set(key, section);
			else if (existing.isObject()) fill((ObjectNode) existing, section, sectionType, policy, node);
		}
	}

	/** Copies defaults into a document wherever it has nothing, descending into sections both have. */
	private void fill(
			ObjectNode target,
			ObjectNode defaults,
			Class<?> type,
			PrimitiveDefaultPolicy policy,
			@Nullable ObjectNode parent
	) {
		for (Map.Entry<String, JsonNode> entry : defaults.properties()) {
			String key = entry.getKey();
			JsonNode existing = target.get(key);
			JsonNode value = entry.getValue();
			if (isMissing(existing, policy)) {
				target.set(key, value.deepCopy());
				continue;
			}

			if (!existing.isObject() || !value.isObject()) continue;

			Field field = SerializedFieldResolver.resolveField(type, key);
			Class<?> childType = SerializedFieldResolver.valueType(type, field);
			if (parent != null)
				childType = documents.resolveType(childType, new DocumentTypeContext(existing, parent, field, key, null, null));

			fill((ObjectNode) existing, (ObjectNode) value, childType, policy, parent == null ? null : target);
		}
	}

	/** Runs the providers of a type and of its superclasses, most general first, over one instance. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private @Nullable JsonNode provided(Class<?> type) {
		Object instance = null;
		for (Class<?> current : hierarchy(type)) {
			DefaultsProvider provider = providers.getProvider((Class) current);
			if (provider == null) continue;

			if (instance == null) instance = instantiate(type);
			Object supplied = provider.supply(instance);
			if (supplied != null) instance = supplied;
		}

		return instance == null ? null : mapper.valueToTree(instance);
	}

	private boolean hasDefaults(Class<?> type) {
		for (Class<?> current : hierarchy(type))
			if (providers.getProvider(current) != null) return true;

		return !SerializedFieldResolver.fields(type).isEmpty();
	}

	private ObjectNode constructed(Class<?> type) {
		if (!hasNoArgumentConstructor(type)) return mapper.createObjectNode();

		return mapper.valueToTree(instantiate(type)) instanceof ObjectNode node ? node : mapper.createObjectNode();
	}

	private static void removeNulls(ObjectNode node) {
		List<String> nulls = new ArrayList<>();
		node.properties().forEach(entry -> {
			if (entry.getValue().isNull()) nulls.add(entry.getKey());
		});
		node.remove(nulls);
	}

	/** A primitive cannot be null, so a zero, a false or an empty list in a model may count as "not set". */
	private static boolean isMissing(@Nullable JsonNode node, PrimitiveDefaultPolicy policy) {
		if (node == null || node.isNull()) return true;
		if (policy != PrimitiveDefaultPolicy.AS_MISSING) return false;

		return (node.isNumber() && node.asDouble() == 0.0)
				|| (node.isBoolean() && !node.asBoolean())
				|| (node.isArray() && node.isEmpty());
	}

	private static boolean isSection(Field field) {
		Class<?> type = field.getType();

		return !type.isArray() && !Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type);
	}

	private static boolean isModel(Class<?> type) {
		if (type == null || type.isPrimitive() || type.isArray() || type.isEnum()) return false;
		if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) return false;
		if (type == Object.class || JsonNode.class.isAssignableFrom(type)) return false;

		return !CharSequence.class.isAssignableFrom(type)
				&& !Number.class.isAssignableFrom(type)
				&& Boolean.class != type
				&& Character.class != type
				&& !Collection.class.isAssignableFrom(type)
				&& !Map.class.isAssignableFrom(type)
				&& !type.getPackageName().startsWith("java.time");
	}

	private static List<Class<?>> hierarchy(Class<?> type) {
		List<Class<?>> hierarchy = new ArrayList<>();
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass())
			hierarchy.add(current);

		Collections.reverse(hierarchy);
		return hierarchy;
	}

	private static boolean hasNoArgumentConstructor(Class<?> type) {
		try {
			type.getDeclaredConstructor();
			return true;
		} catch (NoSuchMethodException missing) {
			return false;
		}
	}

	private static Object instantiate(Class<?> type) {
		try {
			var constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);

			return constructor.newInstance();
		} catch (Exception failure) {
			throw new IllegalStateException("Cannot create " + type.getName() + " to read its defaults", failure);
		}
	}
}
