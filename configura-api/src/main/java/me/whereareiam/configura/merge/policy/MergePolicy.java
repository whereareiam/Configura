package me.whereareiam.configura.merge.policy;

import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolved policy inputs for one merge field.
 */
@RequiredArgsConstructor
public final class MergePolicy {
	private final @Nullable String namedStrategy;
	private final @Nullable Class<? extends FieldMergeStrategy> strategyClass;
	private final @Nullable MergeBehavior behaviorOverride;
	private final @NotNull Map<Class<?>, Object> helperConfigurations;

	/**
	 * Creates an empty policy builder.
	 *
	 * @return policy builder
	 */
	public static @NotNull Builder builder() {
		return new Builder();
	}

	/**
	 * Returns the named merge strategy, if one was configured.
	 *
	 * @return named strategy or {@code null}
	 */
	public @Nullable String getNamedStrategy() {
		return namedStrategy;
	}

	/**
	 * Returns the merge strategy class, if one was configured directly.
	 *
	 * @return merge strategy class or {@code null}
	 */
	public @Nullable Class<? extends FieldMergeStrategy> getStrategyClass() {
		return strategyClass;
	}

	/**
	 * Returns a behavior override contributed by policy resolvers.
	 *
	 * @return behavior override or {@code null}
	 */
	public @Nullable MergeBehavior getBehaviorOverride() {
		return behaviorOverride;
	}

	/**
	 * Returns whether the policy contains a helper configuration of the given type.
	 *
	 * @param type helper configuration type
	 * @return {@code true} when present
	 */
	public boolean hasHelper(@NotNull Class<?> type) {
		return helperConfigurations.containsKey(type);
	}

	/**
	 * Returns a helper configuration for the given type.
	 *
	 * @param type helper configuration type
	 * @param <T> helper configuration type
	 * @return helper configuration or {@code null}
	 */
	@SuppressWarnings("unchecked")
	public <T> @Nullable T helper(@NotNull Class<T> type) {
		return (T) helperConfigurations.get(type);
	}

	/**
	 * Returns the registered helper configuration snapshot.
	 *
	 * @return helper configurations keyed by type
	 */
	public @NotNull Map<Class<?>, Object> helperConfigurations() {
		return helperConfigurations;
	}

	/**
	 * Returns whether the policy contributes no values.
	 *
	 * @return {@code true} when empty
	 */
	public boolean isEmpty() {
		return namedStrategy == null
				&& strategyClass == null
				&& behaviorOverride == null
				&& helperConfigurations.isEmpty();
	}

	/**
	 * Creates a builder pre-populated from this policy.
	 *
	 * @return policy builder
	 */
	public @NotNull Builder toBuilder() {
		Builder builder = new Builder();
		builder.namedStrategy = namedStrategy;
		builder.strategyClass = strategyClass;
		builder.behaviorOverride = behaviorOverride;
		builder.helperConfigurations.putAll(helperConfigurations);
		return builder;
	}

	/**
	 * Builder for {@link MergePolicy}.
	 */
	public static final class Builder {
		private String namedStrategy;
		private Class<? extends FieldMergeStrategy> strategyClass;
		private MergeBehavior behaviorOverride;
		private final Map<Class<?>, Object> helperConfigurations = new LinkedHashMap<>();

		/**
		 * Sets the named merge strategy.
		 *
		 * @param namedStrategy strategy name
		 * @return this builder
		 */
		public @NotNull Builder namedStrategy(@Nullable String namedStrategy) {
			this.namedStrategy = namedStrategy;
			if (namedStrategy != null)
				this.strategyClass = null;
			return this;
		}

		/**
		 * Sets the merge strategy class.
		 *
		 * @param strategyClass merge strategy class
		 * @return this builder
		 */
		public @NotNull Builder strategy(@Nullable Class<? extends FieldMergeStrategy> strategyClass) {
			this.strategyClass = strategyClass;
			if (strategyClass != null)
				this.namedStrategy = null;
			return this;
		}

		/**
		 * Sets a behavior override.
		 *
		 * @param behaviorOverride merge behavior override
		 * @return this builder
		 */
		public @NotNull Builder behaviorOverride(@Nullable MergeBehavior behaviorOverride) {
			this.behaviorOverride = behaviorOverride;
			return this;
		}

		/**
		 * Registers a helper configuration.
		 *
		 * @param type helper configuration type
		 * @param value helper configuration value
		 * @param <T> helper configuration type
		 * @return this builder
		 */
		public <T> @NotNull Builder helper(@NotNull Class<T> type, @NotNull T value) {
			helperConfigurations.put(type, value);
			return this;
		}

		/**
		 * Builds the immutable policy.
		 *
		 * @return immutable policy
		 */
		public @NotNull MergePolicy build() {
			return new MergePolicy(namedStrategy, strategyClass, behaviorOverride, helperConfigurations);
		}
	}
}
