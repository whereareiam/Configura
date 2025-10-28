package me.whereareiam.configura.annotation.template;

import me.whereareiam.configura.TemplateProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a {@link TemplateProvider} to supply a default instance for the
 * annotated field.
 *
 * <p>Example:
 * <pre>{@code
 * class AppConfig {
 * 	@Supplier(AppConfigProvider.class)
 * 	public AppConfig defaults;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Supplier {
	Class<? extends TemplateProvider<?>> value();
}


