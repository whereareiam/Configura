package me.whereareiam.configura.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Polymorphic {
    String discriminator() default "";

    Type[] mappings() default {};

	String defaultValue() default "";

	// Inference-only support
	Infer[] inferBy() default {};

	Class<?> defaultTarget() default Void.class;

	@interface Type {
		String value();

		Class<?> target();
	}

	@interface Infer {
		String field();

		Class<?> target();
	}
}