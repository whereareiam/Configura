package me.whereareiam.configura.common.merge.defaults.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.defaults.FieldDefaultsResolver;

import java.lang.reflect.Field;

public final class InlineListDefaultsResolver implements FieldDefaultsResolver {
	@Override
	public JsonNode resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null) return null;

		if (defaults.items().length > 0) return buildArray(mapper, defaults.items());

		if (defaults.stringItems().length > 0) {
			ArrayNode array = mapper.createArrayNode();
			for (String value : defaults.stringItems())
				array.add(value);

			return array;
		}

		if (defaults.list().items().length > 0)
			return buildArray(mapper, defaults.list().items());

		return null;
	}

	private static ArrayNode buildArray(ObjectMapper mapper, Defaults.Literal[] literals) {
		ArrayNode array = mapper.createArrayNode();
		for (Defaults.Literal literal : literals) {
			Object value = InlineLiteralDefaultsResolver.parseLiteral(literal);
			if (value != null) array.add(mapper.valueToTree(value));
		}
		return array;
	}
}
