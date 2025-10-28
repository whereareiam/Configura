package me.whereareiam.configura;

/**
 * Supplies default template instances for a given configuration type.
 * <p>
 * Implementations are referenced from {@code @Supplier} and are used to
 * provide a base instance when generating or enriching configuration files.
 *
 * <p>Example:
 * <pre>{@code
 * public final class AppConfigProvider implements TemplateProvider<AppConfig> {
 * 	@Override
 * 	public AppConfig supply(Class<AppConfig> targetType) {
 * 		AppConfig cfg = new AppConfig();
 * 		cfg.port = 8080;
 * 		return cfg;
 * 	}
 * }
 * }</pre>
 *
 * @param <T> target configuration type
 */
public interface TemplateProvider<T> {
    T supply(Class<T> targetType);
}


