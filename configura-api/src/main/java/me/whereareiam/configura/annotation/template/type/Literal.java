package me.whereareiam.configura.annotation.template.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a scalar literal value in a template: string, number or boolean.
 * Only one of {@link #text()}, {@link #number()} or {@link #bool()} should be set.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Literal {
	String text() default "";

	String number() default "";

	boolean bool() default false;
}



