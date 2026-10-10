package me.whereareiam.configura.feature.polymorphic;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.document.DocumentTypeResolver;
import me.whereareiam.configura.feature.polymorphic.api.annotation.Polymorphic;
import me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public final class PolymorphicTypeResolver implements DocumentTypeResolver {
	private final DefaultPolymorphicRegistry registry;

	@Override
	public @Nullable Class<?> resolve(@NotNull Class<?> declaredType, @NotNull DocumentTypeContext context) {
		PolymorphicDefinition definition = definition(declaredType);
		if (definition == null) return null;

		Class<?> target = target(definition, context.getCurrentNode());
		return target != null && target != declaredType ? target : null;
	}

	/**
	 * Returns how a base type is resolved: its registration, or else its {@link Polymorphic}
	 * annotation.
	 *
	 * @param declaredType base type
	 * @return definition, or null when the type is not polymorphic
	 */
	public @Nullable PolymorphicDefinition definition(@NotNull Class<?> declaredType) {
		PolymorphicDefinition definition = registry.definition(declaredType);
		if (definition != null) return definition;

		return fromAnnotation(declaredType.getAnnotation(Polymorphic.class));
	}

	/**
	 * Picks the type a document node stands for: the mapping of its discriminator value (or of the
	 * default value when the node has none), else the first inference field it has, else the default
	 * target.
	 *
	 * @param definition definition of the base type
	 * @param node       document node, if any
	 * @return chosen type, which may be the base type itself, or null when nothing applies
	 */
	public @Nullable Class<?> target(@NotNull PolymorphicDefinition definition, @Nullable JsonNode node) {
		Class<?> target = null;

		if (node != null && hasDiscriminator(definition)) {
			String written = discriminatorValue(definition, node);
			String key = written != null ? written : definition.getDefaultValue();
			if (key != null && !key.isEmpty())
				target = definition.getMappings().get(key);
		}

		if (target == null && node != null && definition.getInferFields() != null) {
			for (Map.Entry<String, Class<?>> entry : definition.getInferFields().entrySet()) {
				if (node.has(entry.getKey())) {
					target = entry.getValue();
					break;
				}
			}
		}

		if (target == null && definition.getDefaultTarget() != null && definition.getDefaultTarget() != Void.class)
			target = definition.getDefaultTarget();

		return target;
	}

	/**
	 * Reads the discriminator a node carries.
	 *
	 * @param definition definition of the base type
	 * @param node       document node
	 * @return the text of the discriminator, or null when the node has none or it is not text
	 */
	public static @Nullable String discriminatorValue(@NotNull PolymorphicDefinition definition, @NotNull JsonNode node) {
		if (!hasDiscriminator(definition)) return null;

		JsonNode value = node.get(definition.getDiscriminator());
		return value != null && value.isTextual() ? value.asText() : null;
	}

	private static boolean hasDiscriminator(PolymorphicDefinition definition) {
		return definition.getDiscriminator() != null && !definition.getDiscriminator().isEmpty();
	}

	private static @Nullable PolymorphicDefinition fromAnnotation(@Nullable Polymorphic annotation) {
		if (annotation == null) return null;

		Map<String, Class<?>> mappings = new LinkedHashMap<>();
		for (Polymorphic.Type type : annotation.mappings())
			mappings.put(type.value(), type.target());

		LinkedHashMap<String, Class<?>> inferFields = new LinkedHashMap<>();
		for (Polymorphic.Infer infer : annotation.inferBy())
			inferFields.put(infer.field(), infer.target());

		return new PolymorphicDefinition(
				annotation.discriminator(),
				Collections.unmodifiableMap(mappings),
				annotation.defaultValue(),
				inferFields,
				annotation.defaultTarget()
		);
	}
}
