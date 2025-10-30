package me.whereareiam.configura.common.template.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;

/**
 * Strategy for resolving a template value for a field. Return {@code null} if not applicable.
 */
public interface TemplateValueResolver {
	Object resolve(ObjectMapper mapper, Class<?> targetType, Field field);
}