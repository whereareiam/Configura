package me.whereareiam.configura.common.template.resolvers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import me.whereareiam.configura.annotation.template.Template;
import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.common.template.TemplateValueResolver;

import java.lang.reflect.Field;

public final class ListInlineTemplateResolver implements TemplateValueResolver {
	@Override
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Template t = field.getAnnotation(Template.class);
		if (t == null) return null;

		Literal[] items = t.items();
		if (items == null || items.length == 0) return null;

		ArrayNode arr = mapper.createArrayNode();
		for (Literal it : items) {
			if (!it.text().isEmpty()) {
				arr.add(it.text());
				continue;
			}

			if (!it.number().isEmpty()) {
				try {
					arr.add(Long.parseLong(it.number()));
				} catch (NumberFormatException e) {
					try {
						arr.add(Double.parseDouble(it.number()));
					} catch (NumberFormatException ex) {
						arr.add(it.number());
					}
				}
				continue;
			}

			if (it.bool()) arr.add(true);
		}

		return arr;
	}
}


