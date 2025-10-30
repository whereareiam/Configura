package me.whereareiam.configura.common.template.resolver.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.template.resolver.TemplateValueResolver;

import java.lang.reflect.Field;

public final class ObjectInlineTemplateResolver implements TemplateValueResolver {
	@Override
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Template t = field.getAnnotation(Template.class);
		if (t == null) return null;

		// 1) Top-level Property[] properties()
		Template.Property[] props = t.properties();
		if (props != null && props.length > 0)
			return buildObject(mapper, props);

		// 2) Namespaced Template.Object
		Template.Object obj = t.object();
		if (obj != null) {
			Template.Property[] nprops = obj.properties();
			if (nprops != null && nprops.length > 0)
				return buildObject(mapper, nprops);
		}

		return null;
	}

	private static void addProperty(ObjectMapper mapper, ObjectNode node, Template.Property property) {
		Object value;
		if (!property.text().isEmpty() || !property.number().isEmpty() || property.bool()) {
			value = buildScalar(property.text(), property.number(), property.bool());
		} else {
			Template.Literal literal = property.value();
			value = buildScalar(literal);
		}

		node.set(property.name(), mapper.valueToTree(value));
	}

	private static ObjectNode buildObject(ObjectMapper mapper, Template.Property[] properties) {
		ObjectNode node = mapper.createObjectNode();
		for (Template.Property property : properties)
			addProperty(mapper, node, property);

		return node;
	}

	private static Object buildScalar(Template.Literal s) {
		return buildScalar(s.text(), s.number(), s.bool());
	}

	private static Object buildScalar(String text, String number, boolean boolVal) {
		if (text != null && !text.isEmpty()) return text;

		if (number != null && !number.isEmpty()) {
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

		if (boolVal) return true;

		return null;
	}
}