package me.whereareiam.configura.annotation.template;

import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.annotation.template.type.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an inline template for the annotated field.
 * <p>
 * Use {@link #literal()}, {@link #items()} and {@link #properties()} to define
 * default values that can be materialized into generated configuration files
 * or used during merges.
 *
 * <p>Example for an object with properties:
 * <pre>{@code
 * class DbConfig {
 *    @Template(properties = {
 *        @Property(name = "host", value = @Literal(text = "localhost")),
 *        @Property(name = "port", value = @Literal(number = "5432"))
 *    })
 * 	public Map<String, TemplateObject> defaults;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Template {
	Literal literal() default @Literal;

	Literal[] items() default {};

	Property[] properties() default {};
}