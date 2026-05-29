package me.whereareiam.configura.merge.plugin;

import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.plugin.descriptor.MergeDescriptor;
import me.whereareiam.configura.merge.policy.MergePolicy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered registry of merge field plugins.
 */
public final class MergePluginRegistry {
	private final List<MergePlugin> plugins = new ArrayList<>();

	/**
	 * Creates a copy of this registry.
	 *
	 * @return independent registry copy
	 */
	public @NotNull MergePluginRegistry copy() {
		MergePluginRegistry copy = new MergePluginRegistry();
		copy.plugins.addAll(this.plugins);
		return copy;
	}

	/**
	 * Registers a plugin.
	 *
	 * <p>Resolution scans registered plugins from the most recent registration to the oldest, so
	 * later registrations override earlier ones when both support the same field.</p>
	 *
	 * @param plugin plugin to register
	 * @return this registry
	 */
	public @NotNull MergePluginRegistry register(@NotNull MergePlugin plugin) {
		plugins.add(plugin);
		return this;
	}

	/**
	 * Resolves the plugin that handles the given field.
	 *
	 * @param descriptor field descriptor
	 * @param policy resolved field policy
	 * @return matching plugin
	 */
	public @NotNull MergePlugin resolve(
			@NotNull MergeDescriptor descriptor,
			@NotNull MergePolicy policy
	) {
		for (int index = plugins.size() - 1; index >= 0; index--) {
			MergePlugin plugin = plugins.get(index);
			if (plugin.supports(descriptor, policy))
				return plugin;
		}

		throw new ConfigException("No merge field plugin registered for " + descriptor.getOwnerType().getName() + "#" + descriptor.getSerializedName());
	}

	/**
	 * Returns an immutable snapshot of registered plugins.
	 *
	 * @return registered plugins in registration order
	 */
	public @NotNull List<MergePlugin> asList() {
		return List.copyOf(plugins);
	}
}
