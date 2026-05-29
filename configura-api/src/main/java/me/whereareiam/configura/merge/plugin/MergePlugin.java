package me.whereareiam.configura.merge.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.plugin.context.MergePluginContext;
import me.whereareiam.configura.merge.plugin.context.MergePluginDefaultsContext;
import me.whereareiam.configura.merge.plugin.descriptor.MergeDescriptor;
import me.whereareiam.configura.merge.policy.MergePolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extends merge handling for a particular kind of field.
 */
public interface MergePlugin {
	/**
	 * Returns whether this plugin handles the given field.
	 *
	 * @param descriptor field descriptor
	 * @param policy resolved field policy
	 * @return {@code true} when this plugin should handle the field
	 */
	boolean supports(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy);

	/**
	 * Resolves the effective child type used for recursive merge/default traversal.
	 *
	 * @param descriptor field descriptor
	 * @param policy resolved field policy
	 * @return effective child type
	 */
	@NotNull Class<?> resolveChildType(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy);

	/**
	 * Resolves a defaults node for this field.
	 *
	 * @param context defaults context
	 * @return defaults node, or {@code null} when the plugin contributes no defaults
	 */
	@Nullable JsonNode resolveDefaultValue(@NotNull MergePluginDefaultsContext context);

	/**
	 * Merges one field value.
	 *
	 * @param context merge context
	 * @return merged node to write for the field
	 */
	@NotNull JsonNode merge(@NotNull MergePluginContext context);
}
