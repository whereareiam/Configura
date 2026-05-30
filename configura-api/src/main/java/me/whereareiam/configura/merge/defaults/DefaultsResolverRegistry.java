package me.whereareiam.configura.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.defaults.context.DefaultsContext;
import me.whereareiam.configura.merge.defaults.descriptor.DefaultsDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered registry of defaults resolvers.
 */
public final class DefaultsResolverRegistry {
	private final List<DefaultsResolver> resolvers = new ArrayList<>();

	public @NotNull DefaultsResolverRegistry copy() {
		DefaultsResolverRegistry copy = new DefaultsResolverRegistry();
		copy.resolvers.addAll(resolvers);
		return copy;
	}

	public @NotNull DefaultsResolverRegistry register(@NotNull DefaultsResolver resolver) {
		resolvers.add(resolver);
		return this;
	}

	public @Nullable JsonNode resolve(
			@NotNull DefaultsDescriptor descriptor,
			@NotNull DefaultsContext context
	) {
		for (int index = resolvers.size() - 1; index >= 0; index--) {
			JsonNode resolved = resolvers.get(index).resolve(descriptor, context);
			if (resolved != null) return resolved;
		}
		return null;
	}

	public @NotNull List<DefaultsResolver> asList() {
		return List.copyOf(resolvers);
	}
}
