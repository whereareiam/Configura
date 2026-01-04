package me.whereareiam.configura.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as part of the configuration with merge and validation behavior.
 *
 * <p>Example:
 * <pre>{@code
 * class AppConfig {
 *     @Field(name = "server.port", optional = true)
 *     public Integer port;
 *
 *     @Field(additive = true)
 *     public Map<String, CommandDefinition> commands;
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

	/**
	 * Treats this map as a dynamic field.
	 * Unknown keys in the configuration are routed into the map,
	 * and the map entries are written as top-level properties.
	 *
	 * @return true if field should act as a dynamic key bucket
	 */
	boolean dynamic() default false;

	/**
	 * Whether this field is optional (user can delete it).
	 * When true, setting to null writes explicit {@code null} in YAML,
	 * preventing template from re-applying defaults.
	 *
	 * <p>Example: User sets {@code requirements = null}, saves as {@code requirements: null},
	 * on next load it stays null instead of getting template defaults.
	 *
	 * @return true if field is optional
	 */
	boolean optional() default false;

	/**
	 * Controls how this field is merged with templates.
	 *
	 * @return the merge strategy
	 */
	MergeStrategy merge() default MergeStrategy.DEEP;
}

