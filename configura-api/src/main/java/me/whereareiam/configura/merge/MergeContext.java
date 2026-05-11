package me.whereareiam.configura.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
	private final boolean defaultInstance;
	private final @NotNull RecursiveMerge recursiveMerge;

	/**
	 * Returns whether the source field should be treated as missing for default-instance merging.
	 *
	 * @return {@code true} when Configura should apply defaults over Java primitive defaults
	 */
	public boolean sourceTreatsDefaultAsMissing() {
		if (sourceNode == null) return false;
		if (!defaultInstance) return false;

		return (sourceNode.isNumber() && sourceNode.asDouble() == 0.0)
				|| (sourceNode.isBoolean() && !sourceNode.asBoolean())
				|| (sourceNode.isArray() && sourceNode.isEmpty());
	}

	/**
	 * Recursively merges child fields with the active merge registry.
	 *
	 * @param source source object node
	 * @param defaults default object node
	 * @param ownerType owner type for child strategy resolution
	 * @return merged object node
	 */
	public @NotNull JsonNode mergeChildren(@Nullable JsonNode source, @Nullable JsonNode defaults) {
		return recursiveMerge.merge(source, defaults, childType, false);
	}

	/**
	 * Recursively merges only keys already declared by the source object.
	 *
	 * @param source source object node
	 * @param defaults default object node
	 * @param ownerType owner type for child strategy resolution
	 * @return merged object node containing only source-declared keys
	 */
	public @NotNull JsonNode mergeDeclaredChildren(@Nullable JsonNode source, @Nullable JsonNode defaults) {
		return recursiveMerge.merge(source, defaults, childType, true);
	}

	/**
	 * Callback used by strategies that delegate nested field merging back to the engine.
	 */
	@FunctionalInterface
	public interface RecursiveMerge {
		@NotNull JsonNode merge(@Nullable JsonNode source, @Nullable JsonNode defaults, @NotNull Class<?> ownerType, boolean declaredKeysOnly);
	}
}
