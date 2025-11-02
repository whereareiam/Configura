package me.whereareiam.configura.annotation;

import me.whereareiam.configura.TemplateProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an inline template for the annotated field or class.
 * <p>
 * Use {@link #literal()}, {@link #items()} and {@link #properties()} to define
 * default values that can be materialized into generated configuration files
 * or used during merges.
 *
 * <p>Example for a field with properties:
 * <pre>{@code
 * class DbConfig {
 *    @Template(properties = {
 *        @Template.Property(name = "host", value = @Template.Literal(text = "localhost")),
 *        @Template.Property(name = "port", value = @Template.Literal(number = "5432"))
 *    })
 * 	public Map<String, TemplateObject> defaults;
 * }
 * }</pre>
 *
 * <p>Example for a class-level template:
 * <pre>{@code
 * @Template(supplier = @Template.Supplier(MyConfigProvider.class))
 * class MyConfig {
 *     private String name;
 *     private int port;
 * }
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Template {
	Literal literal() default @Literal;

	Literal[] items() default {};

	Property[] properties() default {};

	/**
	 * Shorthand for a scalar literal. Equivalent to {@link #literal()} with matching field set.
	 */
	String text() default "";

	/**
	 * Shorthand for a numeric literal. Equivalent to {@link #literal()} with number set.
	 */
	String number() default "";

	/**
	 * Shorthand for a boolean literal. Equivalent to {@link #literal()} with bool set.
	 */
	boolean bool() default false;

	/**
	 * Shorthand for list of string literals. Equivalent to {@link #items()} of top-level {@link Literal} with text values.
	 */
	String[] stringItems() default {};

	/**
	 * Namespaced list representation.
	 */
	List list() default @List;

	/**
	 * Namespaced object representation.
	 */
	Object object() default @Object;

	/**
	 * External source (classpath:/, file:, http(s)://). Empty means not set.
	 */
	Source source() default @Source("");

	/**
	 * Provider-driven defaults. Use only if present.
	 */
	Supplier supplier() default @Supplier(Template.Supplier.None.class);

	// --- Nested namespaced annotation types (aliases) ---

	@Retention(RetentionPolicy.RUNTIME)
	@interface Literal {
		String text() default "";

		String number() default "";

		boolean bool() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Property {
		String name();

		Literal value() default @Literal;

		// Shorthands to avoid nested Literal verbosity
		String text() default "";

		String number() default "";

		boolean bool() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		Literal[] items() default {};
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Object {
		Property[] properties() default {};
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Source {
		String value();
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Supplier {
		Class<? extends TemplateProvider<?>> value();

		/**
		 * Marker for default/none supplier to allow an annotation default.
		 */
		final class None implements TemplateProvider<Object> {
			@Override
			public Object supply(Object cfg) {
				return cfg;
			}
		}
	}
}