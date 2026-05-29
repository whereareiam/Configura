package me.whereareiam.configura.common.merge;

import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.jetbrains.annotations.NotNull;

public final class MergeOperation {
	private final @NotNull PrimitiveDefaultPolicy defaultsPolicy;
	private final boolean sourceDefaultsAsMissing;

	private MergeOperation(@NotNull PrimitiveDefaultPolicy defaultsPolicy, boolean sourceDefaultsAsMissing) {
		this.defaultsPolicy = defaultsPolicy;
		this.sourceDefaultsAsMissing = sourceDefaultsAsMissing;
	}

	public static @NotNull MergeOperation userModel() {
		return new MergeOperation(PrimitiveDefaultPolicy.PRESERVE, false);
	}

	public static @NotNull MergeOperation syntheticDefaults(@NotNull MergeBehavior behavior) {
		PrimitiveDefaultPolicy policy = behavior.getPrimitiveDefaultPolicy();
		return new MergeOperation(policy, policy == PrimitiveDefaultPolicy.AS_MISSING);
	}

	public @NotNull PrimitiveDefaultPolicy defaultsPolicy() {
		return defaultsPolicy;
	}

	public boolean sourceDefaultsAsMissing() {
		return sourceDefaultsAsMissing;
	}
}
