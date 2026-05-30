package me.whereareiam.configura.merge.plugin.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.merge.defaults.MergeModelDefaultsResolver;
import me.whereareiam.configura.merge.plugin.descriptor.MergeDescriptor;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inputs passed to a merge field plugin when it contributes defaults.
 */
@RequiredArgsConstructor
public final class MergePluginDefaultsContext {
	private final @NotNull ObjectMapper mapper;
	private final @NotNull MergeDescriptor descriptor;
	private final @NotNull MergePolicy policy;
	private final @NotNull Class<?> childType;
	private final @NotNull PrimitiveDefaultPolicy primitiveDefaultPolicy;
	private final @NotNull MergeModelDefaultsResolver modelDefaultsResolver;

	/**
	 * Returns the object mapper used by the engine.
	 *
	 * @return object mapper
	 */
	public @NotNull ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Returns the current field descriptor.
	 *
	 * @return field descriptor
	 */
	public @NotNull MergeDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Returns the resolved field policy.
	 *
	 * @return resolved field policy
	 */
	public @NotNull MergePolicy getPolicy() {
		return policy;
	}

	/**
	 * Returns the effective child type used for recursive traversal.
	 *
	 * @return child type
	 */
	public @NotNull Class<?> getChildType() {
		return childType;
	}

	/**
	 * Returns the active primitive default policy.
	 *
	 * @return primitive default policy
	 */
	public @NotNull PrimitiveDefaultPolicy getPrimitiveDefaultPolicy() {
		return primitiveDefaultPolicy;
	}

	/**
	 * Resolves registered defaults for the given model type.
	 *
	 * @param type model type
	 * @return resolved defaults node, or {@code null} when none are registered
	 */
	public @Nullable JsonNode resolveModelDefaults(@NotNull Class<?> type) {
		return modelDefaultsResolver.resolve(type);
	}

	/**
	 * Resolves registered defaults for the given model type in the supplied document context.
	 *
	 * @param type model type
	 * @param context current document context
	 * @return resolved defaults node, or {@code null} when none are registered
	 */
	public @Nullable JsonNode resolveModelDefaults(
			@NotNull Class<?> type,
			@Nullable DocumentTypeContext context
	) {
		return modelDefaultsResolver.resolve(type, context);
	}
}
