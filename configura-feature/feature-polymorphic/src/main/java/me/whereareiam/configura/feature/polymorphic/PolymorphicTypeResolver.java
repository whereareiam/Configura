package me.whereareiam.configura.feature.polymorphic;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.document.DocumentTypeResolver;
import me.whereareiam.configura.feature.polymorphic.api.annotation.Polymorphic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public final class PolymorphicTypeResolver implements DocumentTypeResolver {
	private final DefaultPolymorphicRegistry registry;

	@Override
	public @Nullable Class<?> resolve(@NotNull Class<?> declaredType, @NotNull DocumentTypeContext context) {
		me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition definition = effectiveInfoFor(declaredType);
		if (definition == null) return null;

		return resolveTarget(context.getCurrentNode(), declaredType, definition);
	}

	private @Nullable me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition effectiveInfoFor(Class<?> raw) {
		me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition definition = registry.definition(raw);
		if (definition != null) return definition;

		Polymorphic annotation = raw.getAnnotation(Polymorphic.class);
		return fromAnnotation(annotation);
	}

	private static @Nullable Class<?> resolveTarget(JsonNode node, Class<?> raw, me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition definition) {
		Class<?> target = null;

		if (node != null && definition.getDiscriminator() != null && !definition.getDiscriminator().isEmpty()) {
			JsonNode discriminatorValue = node.get(definition.getDiscriminator());
			String key = discriminatorValue != null && discriminatorValue.isTextual()
					? discriminatorValue.asText()
					: definition.getDefaultValue();
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

		return target != null && target != raw ? target : null;
	}

	private static @Nullable me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition fromAnnotation(@Nullable Polymorphic annotation) {
		if (annotation == null) return null;

		Map<String, Class<?>> mappings = new LinkedHashMap<>();
		for (Polymorphic.Type type : annotation.mappings())
			mappings.put(type.value(), type.target());

		LinkedHashMap<String, Class<?>> inferFields = new LinkedHashMap<>();
		for (Polymorphic.Infer infer : annotation.inferBy())
			inferFields.put(infer.field(), infer.target());

		return new me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition(
				annotation.discriminator(),
				Map.copyOf(mappings),
				annotation.defaultValue(),
				inferFields,
				annotation.defaultTarget()
		);
	}
}
