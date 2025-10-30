package me.whereareiam.configura.common.template.resolver.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.template.resolver.TemplateValueResolver;

import java.lang.reflect.Field;

public final class LiteralInlineTemplateResolver implements TemplateValueResolver {
	@Override
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Template t = field.getAnnotation(Template.class);
		if (t == null) return null;

		// Shorthand first
		if (!t.text().isEmpty()) return t.text();
		if (!t.number().isEmpty()) return parseNumber(t.number());
		if (t.bool()) return true;

		Template.Literal s = t.literal();

		return parseLiteral(s);
	}

	private static Object parseNumber(String number) {
		try {
			return Long.parseLong(number);
		} catch (NumberFormatException e) {
			try {
				return Double.parseDouble(number);
			} catch (NumberFormatException ex) {
				return number;
			}
		}
	}

	private static Object parseLiteral(Template.Literal literal) {
		if (!literal.text().isEmpty()) return literal.text();
		if (!literal.number().isEmpty()) return parseNumber(literal.number());
		if (literal.bool()) return true;

		return null;
	}
}