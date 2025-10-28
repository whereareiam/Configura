package me.whereareiam.configura.annotation.template.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents an object value with named {@link Property properties}.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
public @interface TemplateObject {
	Property[] properties() default {};
}


