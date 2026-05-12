package me.whereareiam.configura.annotation;

import me.whereareiam.configura.merge.strategy.MergeStrategy;
import me.whereareiam.configura.merge.strategy.type.DeepDefaults;

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
	Class<? extends MergeStrategy> value() default DeepDefaults.class;

	String named() default "";
}
