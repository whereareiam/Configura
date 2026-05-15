package me.whereareiam.configura.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.merge.strategy.MergeStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Immutable inputs passed to a {@link MergeStrategy}.
 */
@Getter
@RequiredArgsConstructor
public final class MergeContext {
	private final @NotNull ObjectMapper mapper;
	private final @NotNull Class<?> ownerType;
	private final @Nullable Field field;
	private final @NotNull String fieldName;
	private final @NotNull Class<?> childType;
	private final @Nullable JsonNode sourceNode;
	private final @Nullable JsonNode defaultNode;
	@Getter(AccessLevel.NONE)
	private final boolean sourceDefaultsAsMissing;
	private final @NotNull MergeBehavior behavior;
	private final @NotNull RecursiveMerge recursiveMerge;

	/**
	 * Returns whether the current source field should be treated as missing.
	 *
	 * @return {@code true} when the active merge operation should apply defaults over source defaults
	 */
	public boolean sourceTreatsDefaultAsMissing() {
		if (sourceNode == null) return false;
		if (!sourceDefaultsAsMissing) return false;

		return sourceNode.isArray() && sourceNode.isEmpty();
	}

	/**
	 * Recursively merges child fields with the active merge registry.
	 *
	 * @param source source object node
	 * @param defaults default object node
	 * @return merged object node
	 */
	public @NotNull JsonNode mergeChildren(@Nullable JsonNode source, @Nullable JsonNode defaults) {
		return recursiveMerge.merge(source, defaults, childType, false, behavior);
	}

	/**
	 * Recursively merges only keys already declared by the source object.
	 *
	 * @param source source object node
	 * @param defaults default object node
	 * @return merged object node containing only source-declared keys
	 */
	public @NotNull JsonNode mergeDeclaredChildren(@Nullable JsonNode source, @Nullable JsonNode defaults) {
		return recursiveMerge.merge(source, defaults, childType, true, behavior);
	}

	/**
	 * Callback used by strategies that delegate nested field merging back to the engine.
	 */
	@FunctionalInterface
	public interface RecursiveMerge {
		@NotNull JsonNode merge(
				@Nullable JsonNode source,
				@Nullable JsonNode defaults,
				@NotNull Class<?> ownerType,
				boolean declaredKeysOnly,
				@NotNull MergeBehavior behavior
		);
	}
}
