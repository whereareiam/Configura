package me.whereareiam.configura.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuration fields.
 * <p>
 * Allows renaming a Java field to a different key in the configuration file.
 *
 * <p>Example:
 * <pre>{@code
 * class AppConfig {
 * 	@Field(name = "server.port")
 * 	public int port = 8080;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Field {
	/**
	 * Custom path in the configuration file.
	 * If not specified, uses the field name.
	 *
	 * @return the configuration path
	 */
	String name() default "";
}

