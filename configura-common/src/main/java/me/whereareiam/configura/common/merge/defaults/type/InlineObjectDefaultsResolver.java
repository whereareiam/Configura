package me.whereareiam.configura.common.merge.defaults.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.defaults.FieldDefaultsResolver;

import java.lang.reflect.Field;

public final class InlineObjectDefaultsResolver implements FieldDefaultsResolver {
	@Override
	public JsonNode resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null) return null;
		if (defaults.properties().length > 0) return buildObject(mapper, defaults.properties());
		if (defaults.object().properties().length > 0) return buildObject(mapper, defaults.object().properties());

		return null;
	}

	private static ObjectNode buildObject(ObjectMapper mapper, Defaults.Property[] properties) {
		ObjectNode node = mapper.createObjectNode();
		for (Defaults.Property property : properties) {
			Object value = !property.text().isEmpty() || !property.number().isEmpty() || property.bool()
					? InlineLiteralDefaultsResolver.parseLiteral(asLiteral(property.text(), property.number(), property.bool()))
					: InlineLiteralDefaultsResolver.parseLiteral(property.value());
			node.set(property.name(), mapper.valueToTree(value));
		}
		return node;
	}

	private static Defaults.Literal asLiteral(String text, String number, boolean bool) {
		return new Defaults.Literal() {
			@Override
			public String text() {
				return text;
			}

			@Override
			public String number() {
				return number;
			}

			@Override
			public boolean bool() {
				return bool;
			}

			@Override
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return Defaults.Literal.class;
			}
		};
	}
}
