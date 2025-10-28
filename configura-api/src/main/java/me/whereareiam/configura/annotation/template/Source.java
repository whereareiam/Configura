package me.whereareiam.configura.annotation.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated field should be populated from an external
 * template source, such as a classpath resource or URL.
 *
 * <p>Example:
 * <pre>{@code
 * class AppConfig {
 *    @Source("classpath:/defaults/app.yaml")
 *    public Map<String, TemplateObject> defaults;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Source {
	String value();
}
