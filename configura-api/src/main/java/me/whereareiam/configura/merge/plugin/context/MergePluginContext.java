package me.whereareiam.configura.merge.plugin.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.defaults.MergeModelDefaultsResolver;
import me.whereareiam.configura.merge.plugin.descriptor.MergeDescriptor;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.MergeStrategyDefinition;
import me.whereareiam.configura.merge.strategy.capability.StrategyCapabilityKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Runtime inputs passed to a merge field plugin.
 */
@RequiredArgsConstructor
public final class MergePluginContext {
	private final @NotNull ObjectMapper mapper;
	private final @NotNull MergeDescriptor descriptor;
	private final @NotNull MergePolicy policy;
	private final @NotNull Class<?> childType;
	private final @Nullable JsonNode sourceNode;
	private final @Nullable JsonNode defaultNode;
	private final @NotNull MergeStrategyDefinition strategyDefinition;
	private final @Nullable FieldMergeStrategy strategy;
	private final boolean sourceDefaultsAsMissing;
	private final @NotNull MergeBehavior behavior;
	private final @NotNull MergeContext.RecursiveMerge recursiveMerge;
	private final @NotNull MergeModelDefaultsResolver modelDefaultsResolver;
	private final @NotNull BiFunction<Class<?>, DocumentTypeContext, Class<?>> documentTypeResolver;

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
	 * Returns the source node for the field.
	 *
	 * @return source node or {@code null}
	 */
	public @Nullable JsonNode getSourceNode() {
		return sourceNode;
	}

	/**
	 * Returns the defaults node for the field.
	 *
	 * @return defaults node or {@code null}
	 */
	public @Nullable JsonNode getDefaultNode() {
		return defaultNode;
	}

	/**
	 * Returns the resolved merge strategy definition.
	 *
	 * @return strategy definition
	 */
	public @NotNull MergeStrategyDefinition getStrategyDefinition() {
		return strategyDefinition;
	}

	/**
	 * Returns the resolved merge strategy instance.
	 *
	 * @return merge strategy instance or {@code null}
	 */
	public @Nullable FieldMergeStrategy getStrategy() {
		return strategy;
	}

	/**
	 * Returns the resolved merge strategy instance and fails when none is available.
	 *
	 * @return merge strategy instance
	 */
	public @NotNull FieldMergeStrategy requireStrategy() {
		if (strategy == null)
			throw new ConfigException("No merge strategy was resolved for " + descriptor.getOwnerType().getName() + "#" + descriptor.getSerializedName());
		return strategy;
	}

	public <T> @Nullable T capability(@NotNull StrategyCapabilityKey<T> key) {
		return strategyDefinition.capability(key);
	}

	public boolean hasCapability(@NotNull StrategyCapabilityKey<?> key) {
		return strategyDefinition.hasCapability(key);
	}

	/**
	 * Returns the resolved merge behavior for the field.
	 *
	 * @return merge behavior
	 */
	public @NotNull MergeBehavior getBehavior() {
		return behavior;
	}

	/**
	 * Returns the recursive merge callback.
	 *
	 * @return recursive merge callback
	 */
	public @NotNull MergeContext.RecursiveMerge getRecursiveMerge() {
		return recursiveMerge;
	}

	/**
	 * Returns whether the current source node should be treated as missing for defaults purposes.
	 *
	 * @return {@code true} when empty source defaults should be replaced
	 */
	public boolean sourceTreatsDefaultAsMissing() {
		if (sourceNode == null) return false;
		if (!sourceDefaultsAsMissing) return false;

		return sourceNode.isArray() && sourceNode.isEmpty();
	}

	/**
	 * Recursively merges child fields using the active field plugin registry.
	 *
	 * @param source child source node
	 * @param defaults child defaults node
	 * @return merged child node
	 */
	public @NotNull JsonNode mergeChildren(@Nullable JsonNode source, @Nullable JsonNode defaults) {
		return recursiveMerge.merge(source, defaults, childType, false, behavior);
	}

	/**
	 * Recursively merges only source-declared child keys.
	 *
	 * @param source child source node
	 * @param defaults child defaults node
	 * @return merged child node containing only source-declared keys
	 */
	public @NotNull JsonNode mergeDeclaredChildren(@Nullable JsonNode source, @Nullable JsonNode defaults) {
		return recursiveMerge.merge(source, defaults, childType, true, behavior);
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

	/**
	 * Resolves the effective document type for the given declared type in the supplied context.
	 *
	 * @param type declared document type
	 * @param context current document context
	 * @return effective document type
	 */
	public @NotNull Class<?> resolveDocumentType(
			@NotNull Class<?> type,
			@NotNull DocumentTypeContext context
	) {
		return documentTypeResolver.apply(type, context);
	}
}
