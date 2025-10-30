package me.whereareiam.configura.common.template.resolver.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.template.resolver.TemplateValueResolver;

import java.lang.reflect.Field;

public final class ListInlineTemplateResolver implements TemplateValueResolver {
	private static void addLiteral(ArrayNode array, Template.Literal literal) {
		if (!literal.text().isEmpty()) {
			array.add(literal.text());
			return;
		}

		if (!literal.number().isEmpty()) {
			try {
				array.add(Long.parseLong(literal.number()));
			} catch (NumberFormatException e) {
				try {
					array.add(Double.parseDouble(literal.number()));
				} catch (NumberFormatException ex) {
					array.add(literal.number());
				}
			}
			return;
		}

		if (literal.bool()) array.add(true);
	}

	private static ArrayNode buildArrayFromLiterals(ObjectMapper mapper, Template.Literal[] literals) {
		ArrayNode array = mapper.createArrayNode();
		for (Template.Literal literal : literals)
			addLiteral(array, literal);

		return array;
	}

	@Override
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Template t = field.getAnnotation(Template.class);
		if (t == null) return null;

		// 1) Top-level Literal[] items()
		Template.Literal[] items = t.items();
		if (items != null && items.length > 0)
			return buildArrayFromLiterals(mapper, items);

		// 2) Shorthand stringItems()
		String[] strItems = t.stringItems();
		if (strItems != null && strItems.length > 0) {
			ArrayNode arr = mapper.createArrayNode();
			for (String s : strItems)
				arr.add(s);

			return arr;
		}

		// 3) Namespaced Template.List
		Template.List list = t.list();
		if (list != null) {
			Template.Literal[] lits = list.items();
			if (lits != null && lits.length > 0)
				return buildArrayFromLiterals(mapper, lits);
		}

		return null;
	}
}