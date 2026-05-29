package me.whereareiam.configura.merge.plugin.list;

import me.whereareiam.configura.type.merge.tree.list.ListMode;
import me.whereareiam.configura.type.merge.tree.list.ListPresence;
import me.whereareiam.configura.type.merge.tree.list.ListUnknownEntries;
import org.jetbrains.annotations.NotNull;

/**
 * Helper configuration consumed by the built-in list merge plugin.
 */
public final class ListMergeConfig {
	private final @NotNull ListMode mode;
	private final @NotNull String key;
	private final @NotNull ListPresence presence;
	private final @NotNull ListUnknownEntries unknownEntries;

	/**
	 * Creates built-in list merge helper configuration.
	 *
	 * @param mode list merge mode
	 * @param key keyed-list identifier field
	 * @param presence list presence policy
	 * @param unknownEntries unknown-entry handling policy
	 */
	public ListMergeConfig(
			@NotNull ListMode mode,
			@NotNull String key,
			@NotNull ListPresence presence,
			@NotNull ListUnknownEntries unknownEntries
	) {
		this.mode = mode;
		this.key = key;
		this.presence = presence;
		this.unknownEntries = unknownEntries;
	}

	public @NotNull ListMode getMode() {
		return mode;
	}

	public @NotNull String getKey() {
		return key;
	}

	public @NotNull ListPresence getPresence() {
		return presence;
	}

	public @NotNull ListUnknownEntries getUnknownEntries() {
		return unknownEntries;
	}
}
