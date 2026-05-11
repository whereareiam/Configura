package me.whereareiam.configura.common.merge.defaults.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.defaults.FieldDefaultsResolver;

import java.lang.reflect.Field;

public final class InlineLiteralDefaultsResolver implements FieldDefaultsResolver {
	@Override
	public JsonNode resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null) return null;

		Object value;
		if (!defaults.text().isEmpty()) value = defaults.text();
		else if (!defaults.number().isEmpty()) value = parseNumber(defaults.number());
		else if (defaults.bool()) value = true;
		else value = parseLiteral(defaults.literal());

		return value == null ? null : mapper.valueToTree(value);
	}

	static Object parseLiteral(Defaults.Literal literal) {
		if (!literal.text().isEmpty()) return literal.text();
		if (!literal.number().isEmpty()) return parseNumber(literal.number());
		if (literal.bool()) return true;
		return null;
	}

	private static Object parseNumber(String number) {
		try {
			return Long.parseLong(number);
		} catch (NumberFormatException e) {
			try {
				return Double.parseDouble(number);
			} catch (NumberFormatException ignored) {
				return number;
			}
		}
	}
}
