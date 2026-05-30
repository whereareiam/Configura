package me.whereareiam.configura.merge.type.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.defaults.MergeModelDefaultsResolver;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.MergeStrategyDefinition;
import me.whereareiam.configura.merge.strategy.capability.StrategyCapabilityKey;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Runtime inputs passed to a merge type adapter.
 */
@RequiredArgsConstructor
public final class MergeTypeAdapterContext {
	private final @NotNull ObjectMapper mapper;
	private final @NotNull MergeTypeDescriptor descriptor;
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

	public @NotNull ObjectMapper getMapper() {
		return mapper;
	}

	public @NotNull MergeTypeDescriptor getDescriptor() {
		return descriptor;
	}

	public @NotNull MergePolicy getPolicy() {
		return policy;
	}

	public @NotNull Class<?> getChildType() {
		return childType;
	}

	public @Nullable JsonNode getSourceNode() {
		return sourceNode;
	}

	public @Nullable JsonNode getDefaultNode() {
		return defaultNode;
	}

	public @NotNull MergeStrategyDefinition getStrategyDefinition() {
		return strategyDefinition;
	}

	public @Nullable FieldMergeStrategy getStrategy() {
		return strategy;
	}

	public @NotNull FieldMergeStrategy requireStrategy() {
		if (strategy == null)
			throw new IllegalStateException("No merge strategy resolved for " + descriptor.getOwnerType().getName() + "#" + descriptor.getSerializedName());
		return strategy;
	}

	public boolean sourceTreatsDefaultAsMissing() {
		if (sourceNode == null || !sourceDefaultsAsMissing) return false;
		return sourceNode.isArray() && sourceNode.isEmpty();
	}

	public @NotNull MergeBehavior getBehavior() {
		return behavior;
	}

	public @NotNull MergeContext.RecursiveMerge getRecursiveMerge() {
		return recursiveMerge;
	}

	public @NotNull JsonNode mergeChildren(@Nullable JsonNode source, @Nullable JsonNode defaults) {
		return recursiveMerge.merge(source, defaults, childType, false, behavior);
	}

	public @NotNull JsonNode mergeDeclaredChildren(@Nullable JsonNode source, @Nullable JsonNode defaults) {
		return recursiveMerge.merge(source, defaults, childType, true, behavior);
	}

	public @Nullable JsonNode resolveModelDefaults(@NotNull Class<?> type) {
		return modelDefaultsResolver.resolve(type);
	}

	public @Nullable JsonNode resolveModelDefaults(@NotNull Class<?> type, @Nullable DocumentTypeContext context) {
		return modelDefaultsResolver.resolve(type, context);
	}

	public @NotNull Class<?> resolveDocumentType(@NotNull Class<?> type, @NotNull DocumentTypeContext context) {
		return documentTypeResolver.apply(type, context);
	}

	public <T> @Nullable T capability(@NotNull StrategyCapabilityKey<T> key) {
		return strategyDefinition.capability(key);
	}

	public boolean hasCapability(@NotNull StrategyCapabilityKey<?> key) {
		return strategyDefinition.hasCapability(key);
	}

	public @Nullable JsonNode objectMember(@Nullable JsonNode node, @NotNull String key) {
		return node != null && node.isObject() ? node.get(key) : null;
	}
}
