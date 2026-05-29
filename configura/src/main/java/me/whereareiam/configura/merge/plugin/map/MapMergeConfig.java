package me.whereareiam.configura.merge.plugin.map;

import me.whereareiam.configura.type.merge.tree.map.MapPresence;
import me.whereareiam.configura.type.merge.tree.map.MapUnknownEntries;
import org.jetbrains.annotations.NotNull;

/**
 * Helper configuration consumed by the built-in map merge plugin.
 */
public final class MapMergeConfig {
	private final @NotNull MapPresence presence;
	private final @NotNull MapUnknownEntries unknownEntries;

	/**
	 * Creates built-in map merge helper configuration.
	 *
	 * @param presence map presence policy
	 * @param unknownEntries unknown-entry handling policy
	 */
	public MapMergeConfig(
			@NotNull MapPresence presence,
			@NotNull MapUnknownEntries unknownEntries
	) {
		this.presence = presence;
		this.unknownEntries = unknownEntries;
	}

	public @NotNull MapPresence getPresence() {
		return presence;
	}

	public @NotNull MapUnknownEntries getUnknownEntries() {
		return unknownEntries;
	}
}
