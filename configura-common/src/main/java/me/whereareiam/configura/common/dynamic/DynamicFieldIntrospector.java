package me.whereareiam.configura.common.dynamic;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import me.whereareiam.configura.annotation.Field;

import java.util.Map;

/**
 * Treats @Field(dynamic = true) maps as JsonAnyGetter/JsonAnySetter targets.
 */
public class DynamicFieldIntrospector extends JacksonAnnotationIntrospector {
	@Override
	public Boolean hasAnyGetter(Annotated annotated) {
		if (isDynamicMap(annotated)) return Boolean.TRUE;
		return super.hasAnyGetter(annotated);
	}

	@Override
	public Boolean hasAnySetter(Annotated annotated) {
		if (isDynamicMap(annotated)) return Boolean.TRUE;
		return super.hasAnySetter(annotated);
	}

	@Override
	public boolean hasIgnoreMarker(AnnotatedMember member) {
		if (isDynamicGetter(member)) return true;
		return super.hasIgnoreMarker(member);
	}

	private boolean isDynamicMap(Annotated annotated) {
		if (!(annotated instanceof AnnotatedMember member)) return false;

		Field fieldAnnotation = member.getAnnotation(Field.class);
		if (fieldAnnotation == null || !fieldAnnotation.dynamic()) return false;

		Class<?> raw = member.getRawType();
		return raw != null && Map.class.isAssignableFrom(raw);
	}

	private boolean isDynamicGetter(AnnotatedMember member) {
		if (!(member instanceof AnnotatedMethod method)) return false;

		String property = getterPropertyName(method.getName());
		if (property == null || property.isBlank()) return false;

		try {
			java.lang.reflect.Field field = method.getDeclaringClass().getDeclaredField(property);
			Field fieldAnnotation = field.getAnnotation(Field.class);
			return fieldAnnotation != null && fieldAnnotation.dynamic()
					&& Map.class.isAssignableFrom(field.getType());
		} catch (NoSuchFieldException ignored) {
			return false;
		}
	}

	private String getterPropertyName(String methodName) {
		if (methodName == null) return null;

		if (methodName.startsWith("get") && methodName.length() > 3) {
			return decapitalize(methodName.substring(3));
		}
		if (methodName.startsWith("is") && methodName.length() > 2) {
			return decapitalize(methodName.substring(2));
		}

		return null;
	}

	private String decapitalize(String value) {
		if (value == null || value.isEmpty()) return value;
		if (value.length() == 1) return value.toLowerCase();
		return Character.toLowerCase(value.charAt(0)) + value.substring(1);
	}
}
