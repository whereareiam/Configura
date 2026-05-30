package me.whereareiam.configura.merge.policy;

import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves policy contributions for a merge field.
 */
public interface MergePolicyResolver {
	/**
	 * Resolves a policy contribution for the given field.
	 *
	 * @param descriptor field descriptor
	 * @return policy contribution, or {@code null} when this resolver does not contribute
	 */
	@Nullable MergePolicy resolve(@NotNull MergeTypeDescriptor descriptor);
}
