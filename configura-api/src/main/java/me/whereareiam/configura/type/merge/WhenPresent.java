package me.whereareiam.configura.type.merge;

/**
 * What happens to a field the file has.
 */
public enum WhenPresent {
	/** The user's values are kept, and what is missing below the field is filled in from the default. */
	FILL_MISSING,

	/** The field is kept exactly as the user wrote it. */
	KEEP_AS_WRITTEN
}
