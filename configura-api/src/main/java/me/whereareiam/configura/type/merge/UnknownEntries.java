package me.whereareiam.configura.type.merge;

/**
 * What happens to the entries of a user's map or keyed list that the default does not contain.
 */
public enum UnknownEntries {
	/** They are accepted. */
	ALLOW,

	/** Loading fails, naming the entry. */
	REJECT
}
