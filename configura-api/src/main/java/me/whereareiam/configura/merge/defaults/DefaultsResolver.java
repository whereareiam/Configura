package me.whereareiam.configura.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.defaults.context.DefaultsContext;
import me.whereareiam.configura.merge.defaults.descriptor.DefaultsDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves defaults for a field or type.
 */
public interface DefaultsResolver {
	@Nullable JsonNode resolve(@NotNull DefaultsDescriptor descriptor, @NotNull DefaultsContext context);
}
