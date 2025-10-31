package me.whereareiam.configura.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares merge behavior for smart update/save operations.
 *
 * <p>Semantics:</p>
 * <ul>
 *   <li>{@link #mergeOnUpdate()} — when false at type level, avoid rewriting and load as-is.</li>
 *   <li>{@link #skipMerge()} — when true on a field, do not add missing defaults for that field.</li>
 *   <li>{@link #preserveWrite()} — when true on a field, if user content exists, keep that subtree as-is (no deep merge).</li>
 * </ul>
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Policy {
	/**
	 * When false at type level, updateRead will not rewrite the file; it will load as-is.
	 */
	boolean mergeOnUpdate() default true;

	/**
	 * Field-level: do not add missing defaults for this field when merging.
	 */
	boolean skipMerge() default false;

	/**
	 * Field-level: if existing file contains this field, keep its subtree as-is (no deep merge).
	 */
	boolean preserveWrite() default false;
}


