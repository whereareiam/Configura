package me.whereareiam.configura.annotation.template.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Declares a named property in an {@link TemplateObject} template with a literal value.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {
	String name();

	Literal value() default @Literal;
}


