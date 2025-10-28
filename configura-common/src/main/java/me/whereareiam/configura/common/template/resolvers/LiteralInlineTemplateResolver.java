package me.whereareiam.configura.common.template.resolvers;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.template.Template;
import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.common.template.TemplateValueResolver;

import java.lang.reflect.Field;

public final class LiteralInlineTemplateResolver implements TemplateValueResolver {
	@Override
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Template t = field.getAnnotation(Template.class);
		if (t == null) return null;

		Literal s = t.literal();
		if (!s.text().isEmpty()) return s.text();
		if (!s.number().isEmpty()) {
			try {
				return Long.parseLong(s.number());
			} catch (NumberFormatException e) {
				try {
					return Double.parseDouble(s.number());
				} catch (NumberFormatException ex) {
					return s.number();
				}
			}
		}

		if (s.bool()) return true;

		return null;
	}
}


