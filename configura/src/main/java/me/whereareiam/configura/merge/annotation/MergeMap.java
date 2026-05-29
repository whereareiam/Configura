package me.whereareiam.configura.merge.annotation;

import me.whereareiam.configura.type.merge.tree.map.MapPresence;
import me.whereareiam.configura.type.merge.tree.map.MapUnknownEntries;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares map-specific merge behavior for map fields.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MergeMap {
	MapPresence presence() default MapPresence.DECLARED_ONLY;

	MapUnknownEntries unknownEntries() default MapUnknownEntries.ALLOW;
}
