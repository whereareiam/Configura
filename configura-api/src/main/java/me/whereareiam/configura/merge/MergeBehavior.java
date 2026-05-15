package me.whereareiam.configura.merge;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import me.whereareiam.configura.type.UnknownFieldPolicy;
import org.jetbrains.annotations.NotNull;

/**
 * Instance-wide merge behavior shared by merge, update, and save flows.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class MergeBehavior {
	private final @NotNull PrimitiveDefaultPolicy primitiveDefaultPolicy;
	private final @NotNull UnknownFieldPolicy unknownFieldPolicy;

	/**
	 * Returns the default merge behavior used by Configura instances.
	 *
	 * @return default merge behavior
	 */
	public static @NotNull MergeBehavior defaults() {
		return builder().build();
	}

	/**
	 * Creates a builder for merge behavior.
	 *
	 * @return merge behavior builder
	 */
	public static @NotNull Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for merge behavior values.
	 */
	public static final class Builder {
		private PrimitiveDefaultPolicy primitiveDefaultPolicy = PrimitiveDefaultPolicy.AS_MISSING;
		private UnknownFieldPolicy unknownFieldPolicy = UnknownFieldPolicy.DROP;

		/**
		 * Sets how primitive defaults from synthetic model instances are materialized during update-like merges.
		 *
		 * @param primitiveDefaultPolicy primitive default handling policy
		 * @return this builder
		 */
		public Builder primitiveDefaults(@NotNull PrimitiveDefaultPolicy primitiveDefaultPolicy) {
			this.primitiveDefaultPolicy = primitiveDefaultPolicy;
			return this;
		}

		/**
		 * Sets how unknown source keys are handled during merge/update.
		 *
		 * @param unknownFieldPolicy unknown field handling policy
		 * @return this builder
		 */
		public Builder unknownFields(@NotNull UnknownFieldPolicy unknownFieldPolicy) {
			this.unknownFieldPolicy = unknownFieldPolicy;
			return this;
		}

		/**
		 * Builds an immutable merge behavior value.
		 *
		 * @return merge behavior value
		 */
		public @NotNull MergeBehavior build() {
			return new MergeBehavior(primitiveDefaultPolicy, unknownFieldPolicy);
		}
	}
}
