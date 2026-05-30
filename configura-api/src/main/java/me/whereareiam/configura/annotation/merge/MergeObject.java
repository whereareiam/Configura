package me.whereareiam.configura.annotation.merge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares object/subtree defaults for a field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MergeObject {
	Property[] properties() default {};

	@Retention(RetentionPolicy.RUNTIME)
	@interface Property {
		String name();

		String text() default "";

		String number() default "";

		boolean bool() default false;
	}
}
