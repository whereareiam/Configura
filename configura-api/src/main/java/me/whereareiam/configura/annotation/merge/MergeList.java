package me.whereareiam.configura.annotation.merge;

import me.whereareiam.configura.type.merge.tree.list.ListMode;
import me.whereareiam.configura.type.merge.tree.list.ListPresence;
import me.whereareiam.configura.type.merge.tree.list.ListUnknownEntries;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares list-specific merge behavior and list item defaults.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MergeList {
	ListMode mode() default ListMode.PLAIN;

	String key() default "";

	ListPresence presence() default ListPresence.DECLARED_ONLY;

	ListUnknownEntries unknownEntries() default ListUnknownEntries.ALLOW;

	Item[] items() default {};

	@Retention(RetentionPolicy.RUNTIME)
	@interface Item {
		String text() default "";

		String number() default "";

		boolean bool() default false;

		MergeObject.Property[] properties() default {};
	}
}
