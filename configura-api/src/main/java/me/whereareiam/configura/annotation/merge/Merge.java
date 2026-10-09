package me.whereareiam.configura.annotation.merge;

import me.whereareiam.configura.type.merge.MissingEntries;
import me.whereareiam.configura.type.merge.UnknownEntries;
import me.whereareiam.configura.type.merge.WhenAbsent;
import me.whereareiam.configura.type.merge.WhenPresent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Says how the defaults of a field meet what a user wrote in the file. A field without the
 * annotation behaves like one with all values left at their defaults: the default is written when
 * the file lacks the field, and missing parts are filled in when the file has it.
 *
 * <pre>{@code
 * @Merge(absent = WhenAbsent.LEAVE_ABSENT)          // an optional section, filled once the user adds it
 * private Session session;
 *
 * @Merge(key = "id", missingEntries = MissingEntries.OMIT)   // entries matched by their id
 * private List<ProviderEntry> providers;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Merge {
	/**
	 * What to do when the file does not have the field.
	 *
	 * @return whether the default is written
	 */
	WhenAbsent absent() default WhenAbsent.ADD_DEFAULT;

	/**
	 * What to do when the file has the field.
	 *
	 * @return whether what the user left out below it is filled in from the default
	 */
	WhenPresent present() default WhenPresent.FILL_MISSING;

	/**
	 * For a list of objects: the property that identifies an entry, so that the entries of the file
	 * and of the default are matched by it and merged one by one. Without it a list in the file is
	 * taken as a whole.
	 *
	 * @return name of the identifying property, or empty for a list taken as a whole
	 */
	String key() default "";

	/**
	 * For a map or a keyed list the file has: what to do with default entries the user does not
	 * have.
	 *
	 * @return whether they are added
	 */
	MissingEntries missingEntries() default MissingEntries.ADD;

	/**
	 * For a map or a keyed list: what to do with entries of the file that the default does not have.
	 *
	 * @return whether they are accepted
	 */
	UnknownEntries unknownEntries() default UnknownEntries.ALLOW;
}
