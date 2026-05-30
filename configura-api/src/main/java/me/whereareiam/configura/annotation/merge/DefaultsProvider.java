package me.whereareiam.configura.annotation.merge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a provider-backed defaults source for a field or type.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultsProvider {
	Class<? extends me.whereareiam.configura.merge.defaults.DefaultsProvider<?>> value();
}
