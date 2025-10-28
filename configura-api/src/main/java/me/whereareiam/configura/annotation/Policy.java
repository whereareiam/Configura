package me.whereareiam.configura.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares merge behavior for {@code updateRead} operations.
 *
 * <p>When {@link #mergeOnUpdate()} is true (default), the framework will
 * merge defaults and templates into the file, pruning unknown fields and
 * adding missing ones. When false, the file is not rewritten and is loaded
 * as-is.
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Policy {
	/**
	 * When true, updateRead will merge new fields (from model/templates) and prune unknown fields.
	 * When false, updateRead will not rewrite the file; it will load as-is.
	 */
	boolean mergeOnUpdate() default true;
}


