package me.whereareiam.configura.annotation.merge;

import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares merge strategy selection and common merge options for a field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Merge {
	Class<? extends FieldMergeStrategy> value() default FieldMergeStrategy.class;

	String named() default "";
}
