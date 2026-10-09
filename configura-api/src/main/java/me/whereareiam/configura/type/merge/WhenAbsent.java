package me.whereareiam.configura.type.merge;

/**
 * What happens to a field the file does not have.
 */
public enum WhenAbsent {
	/** The default is written into the file. */
	ADD_DEFAULT,

	/** The field stays out of the file until the user adds it. */
	LEAVE_ABSENT
}
