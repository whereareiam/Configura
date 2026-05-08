package me.whereareiam.configura.annotation;

import me.whereareiam.configura.type.MergePreset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares merge behavior for a field when template defaults are applied during merge/update.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Merge {
	MergePreset preset() default MergePreset.DEEP_DEFAULTS;

	String policy() default "";
}
