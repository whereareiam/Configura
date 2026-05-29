package me.whereareiam.configura.merge.annotation;

import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.merge.strategy.DeepDefaults;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares merge behavior for a field when defaults from {@link Defaults} are applied during merge/update.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Merge {
	Class<? extends FieldMergeStrategy> value() default DeepDefaults.class;

	String named() default "";
}
