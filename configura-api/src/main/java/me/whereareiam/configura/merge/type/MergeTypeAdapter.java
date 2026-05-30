package me.whereareiam.configura.merge.type;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Teaches Configura how a declared type or type family should merge.
 */
public interface MergeTypeAdapter {
	boolean supports(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy);

	@NotNull Class<?> resolveChildType(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy);

	@NotNull JsonNode merge(@NotNull MergeTypeAdapterContext context);
}
