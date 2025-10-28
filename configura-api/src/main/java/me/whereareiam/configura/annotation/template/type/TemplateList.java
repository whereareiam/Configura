package me.whereareiam.configura.annotation.template.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a list value in a template as a sequence of {@link Literal} items.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
public @interface TemplateList {
	Literal[] items() default {};
}


