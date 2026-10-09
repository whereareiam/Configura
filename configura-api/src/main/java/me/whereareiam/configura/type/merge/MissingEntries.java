package me.whereareiam.configura.type.merge;

/**
 * What happens to the default entries of a map or keyed list that the user's own does not contain.
 */
public enum MissingEntries {
	/** They are added to the user's entries. */
	ADD,

	/** They are left out; the user's entries are the whole collection. */
	OMIT
}
