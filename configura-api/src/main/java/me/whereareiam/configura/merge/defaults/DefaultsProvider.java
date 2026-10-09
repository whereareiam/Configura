package me.whereareiam.configura.merge.defaults;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supplies the defaults of a model type in code, for defaults that field initializers cannot
 * express or that should live apart from the model.
 *
 * <p>Register a provider with {@code Configura.builder().defaults(...)} or
 * {@code configura.withDefaults(...)}. Configura constructs a model, passes it to the provider and
 * takes what comes back as defaults. Providers of superclasses run first. The provider needs an
 * accessible no-argument constructor.</p>
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
