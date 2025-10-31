package me.whereareiam.configura.common.util;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import me.whereareiam.configura.annotation.Field;

public final class BeanPropertyUtil {
    private BeanPropertyUtil() {}

    public static String computeKey(BeanPropertyDefinition prop) {
        if (prop == null) return null;
        AnnotatedMember member = prop.getPrimaryMember();
        if (member == null) return prop.getName();
        Field cf = member.getAnnotation(Field.class);
        if (cf != null && !cf.name().isEmpty()) return cf.name();
        return prop.getName();
    }
}


