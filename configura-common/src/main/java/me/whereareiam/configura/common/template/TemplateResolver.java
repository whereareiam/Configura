package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.common.template.resolver.TemplateValueResolver;
import me.whereareiam.configura.common.template.resolver.type.*;
import me.whereareiam.configura.template.TemplateRegistry;

import java.lang.reflect.Field;
import java.util.List;

public final class TemplateResolver {
    public static Object resolveFieldTemplate(ObjectMapper mapper, TemplateRegistry templateRegistry, Class<?> targetType, Field field) {
        List<TemplateValueResolver> chain = List.of(
                new SupplierTemplateValueResolver(),
                new SourceTemplateValueResolver(),
                new LiteralInlineTemplateResolver(),
                new ListInlineTemplateResolver(),
                new ObjectInlineTemplateResolver(),
                new ModelTemplateValueResolver(templateRegistry)
        );

        for (TemplateValueResolver r : chain) {
            Object v = r.resolve(mapper, targetType, field);
            if (v != null) return v;
        }

        return null;
    }
}