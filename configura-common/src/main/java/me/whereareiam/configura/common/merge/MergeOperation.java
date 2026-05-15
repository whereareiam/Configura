package me.whereareiam.configura.common.merge;

import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.jetbrains.annotations.NotNull;

final class MergeOperation {
	private final @NotNull PrimitiveDefaultPolicy defaultsPolicy;
	private final boolean sourceDefaultsAsMissing;

	private MergeOperation(@NotNull PrimitiveDefaultPolicy defaultsPolicy, boolean sourceDefaultsAsMissing) {
		this.defaultsPolicy = defaultsPolicy;
		this.sourceDefaultsAsMissing = sourceDefaultsAsMissing;
	}

	static @NotNull MergeOperation userModel() {
		return new MergeOperation(PrimitiveDefaultPolicy.PRESERVE, false);
	}

	static @NotNull MergeOperation syntheticDefaults(@NotNull MergeBehavior behavior) {
		PrimitiveDefaultPolicy policy = behavior.getPrimitiveDefaultPolicy();
		return new MergeOperation(policy, policy == PrimitiveDefaultPolicy.AS_MISSING);
	}

	@NotNull PrimitiveDefaultPolicy defaultsPolicy() {
		return defaultsPolicy;
	}

	boolean sourceDefaultsAsMissing() {
		return sourceDefaultsAsMissing;
	}
}
