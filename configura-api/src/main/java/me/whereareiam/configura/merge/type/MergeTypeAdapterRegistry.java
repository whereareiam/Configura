package me.whereareiam.configura.merge.type;

import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered registry of merge type adapters.
 */
public final class MergeTypeAdapterRegistry {
	private final List<MergeTypeAdapter> adapters = new ArrayList<>();

	public @NotNull MergeTypeAdapterRegistry copy() {
		MergeTypeAdapterRegistry copy = new MergeTypeAdapterRegistry();
		copy.adapters.addAll(adapters);
		return copy;
	}

	public @NotNull MergeTypeAdapterRegistry register(@NotNull MergeTypeAdapter adapter) {
		adapters.add(adapter);
		return this;
	}

	public @NotNull MergeTypeAdapter resolve(
			@NotNull MergeTypeDescriptor descriptor,
			@NotNull MergePolicy policy
	) {
		for (int index = adapters.size() - 1; index >= 0; index--) {
			MergeTypeAdapter adapter = adapters.get(index);
			if (adapter.supports(descriptor, policy))
				return adapter;
		}
		throw new ConfigException("No merge type adapter registered for " + descriptor.getOwnerType().getName() + "#" + descriptor.getSerializedName());
	}

	public @NotNull List<MergeTypeAdapter> asList() {
		return List.copyOf(adapters);
	}
}
