package me.whereareiam.configura.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares merge behavior for template defaults.
 *
 * <p>Controls how template-provided defaults are merged with existing user configuration.</p>
 * <ul>
 *   <li>{@link MergeStrategy#DEFAULT} — Deep merge, adds all missing keys from templates</li>
 *   <li>{@link MergeStrategy#MAP_ADDITIVE_ONLY} — Only apply template if field is missing; allows permanent deletion of entries</li>
 *   <li>{@link MergeStrategy#SKIP} — Don't apply any template defaults for this field</li>
 * </ul>
 *
 * @see MergeStrategy
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Policy {
	/**
	 * Defines the merge strategy for template defaults.
	 *
	 * <p>Use {@link MergeStrategy#MAP_ADDITIVE_ONLY} for Maps where users should be able to
	 * permanently delete entries without them reappearing from templates.</p>
	 *
	 * @return the merge strategy to use
	 */
	MergeStrategy value() default MergeStrategy.DEFAULT;
}


