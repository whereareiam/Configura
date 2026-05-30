package me.whereareiam.configura.merge.policy;

import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered registry of merge field policy resolvers.
 */
public final class MergePolicyResolverRegistry {
	private final List<MergePolicyResolver> resolvers = new ArrayList<>();

	/**
	 * Creates a copy of this registry.
	 *
	 * @return independent registry copy
	 */
	public @NotNull MergePolicyResolverRegistry copy() {
		MergePolicyResolverRegistry copy = new MergePolicyResolverRegistry();
		copy.resolvers.addAll(this.resolvers);
		return copy;
	}

	/**
	 * Registers a resolver.
	 *
	 * <p>Policy contributions are applied in registration order. Later resolvers override earlier
	 * contributions for the same strategy selection or helper configuration key.</p>
	 *
	 * @param resolver resolver to register
	 * @return this registry
	 */
	public @NotNull MergePolicyResolverRegistry register(@NotNull MergePolicyResolver resolver) {
		resolvers.add(resolver);
		return this;
	}

	/**
	 * Resolves the effective policy for a field.
	 *
	 * @param descriptor field descriptor
	 * @return resolved policy
	 */
	public @NotNull MergePolicy resolve(@NotNull MergeTypeDescriptor descriptor) {
		MergePolicy.Builder builder = MergePolicy.builder();

		for (MergePolicyResolver resolver : resolvers) {
			MergePolicy contribution = resolver.resolve(descriptor);
			if (contribution == null || contribution.isEmpty()) continue;

			if (contribution.getNamedStrategy() != null)
				builder.namedStrategy(contribution.getNamedStrategy());
			if (contribution.getStrategyClass() != null)
				builder.strategy(contribution.getStrategyClass());

			if (contribution.getBehaviorOverride() != null)
				builder.behaviorOverride(contribution.getBehaviorOverride());

			for (var entry : contribution.helperConfigurations().entrySet())
				putHelper(builder, entry.getKey(), entry.getValue());
		}

		return builder.build();
	}

	@SuppressWarnings("unchecked")
	private static void putHelper(MergePolicy.Builder builder, Class<?> type, Object value) {
		builder.helper((Class<Object>) type, value);
	}

	/**
	 * Returns an immutable snapshot of registered resolvers.
	 *
	 * @return registered resolvers in registration order
	 */
	public @NotNull List<MergePolicyResolver> asList() {
		return List.copyOf(resolvers);
	}
}
