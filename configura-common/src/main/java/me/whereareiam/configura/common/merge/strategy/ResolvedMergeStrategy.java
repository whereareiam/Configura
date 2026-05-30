package me.whereareiam.configura.common.merge.strategy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.MergeStrategyDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolved merge strategy definition paired with its instantiated strategy.
 */
@Getter
@RequiredArgsConstructor
public final class ResolvedMergeStrategy {
	private final @NotNull MergeStrategyDefinition definition;
	private final @Nullable FieldMergeStrategy strategy;
}
