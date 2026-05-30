package me.whereareiam.configura.merge.defaults;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supplies model defaults that are consumed by merge strategies.
 *
 * <p>Providers are registered with {@code Config.builder().defaults(...)} or referenced by
 * {@code @Defaults(provider = @Defaults.Provider(...))}. Configura creates an empty model instance,
 * passes it to the provider, and treats the returned model as the default node for merge.</p>
 *
 * <pre>{@code
 * public final class AppDefaults implements DefaultsProvider<AppConfig> {
 *     @Override
 *     public AppConfig supply(AppConfig config) {
 *         config.name = "app";
 *         return config;
 *     }
 * }
 * }</pre>
 *
 * @param <T> model type this provider supplies defaults for
 */
public interface DefaultsProvider<T> {
	/**
	 * Populates and returns defaults for the given model instance.
	 *
	 * @param config empty model instance created by Configura
	 * @return populated defaults, or {@code null} when no defaults should be applied
	 */
	@Nullable T supply(@NotNull T config);
}
