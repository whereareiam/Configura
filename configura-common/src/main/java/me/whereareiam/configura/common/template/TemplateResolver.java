package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.common.template.resolvers.*;

import java.lang.reflect.Field;
import java.util.List;

public final class TemplateResolver {
	private static final List<TemplateValueResolver> DEFAULT_CHAIN = List.of(
			new SupplierTemplateValueResolver(),
			new SourceTemplateValueResolver(),
			new LiteralInlineTemplateResolver(),
			new ListInlineTemplateResolver(),
			new ObjectInlineTemplateResolver(),
			new ModelTemplateValueResolver()
	);

	public static Object resolveFieldTemplate(ObjectMapper mapper, Class<?> targetType, Field field) {
		for (TemplateValueResolver r : DEFAULT_CHAIN) {
			Object v = r.resolve(mapper, targetType, field);
			if (v != null) return v;
		}

		return null;
	}

}


