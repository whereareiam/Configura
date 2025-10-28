package me.whereareiam.configura.reader;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.type.Format;

import java.util.ServiceLoader;

/**
 * Reads configuration from files.
 * <p>
 * Use this when you need explicit control over format.
 * Configure once and reuse for multiple files.
 */
public interface ConfigReader {
	/**
	 * Creates a new ConfigReader.
	 * Implementation provided by configura-common.
	 *
	 * @return a new ConfigReader instance
	 */
	static ConfigReader create() {
		ServiceLoader<ConfigReaderProvider> loader = ServiceLoader.load(ConfigReaderProvider.class);
		for (ConfigReaderProvider p : loader)
			return p.create();

		throw new UnsupportedOperationException("No ConfigReaderProvider found. Add configura-common to the classpath.");
	}

	/**
	 * Configure the file format to use.
	 *
	 * @param format the format (YAML or JSON)
	 * @return this reader for chaining
	 */
	ConfigReader withFormat(Format format);

	/**
	 * Register a custom type adapter.
	 *
	 * @param type         the type to adapt
	 * @param adapterClass the adapter class
	 * @param <T>          the type
	 * @return this reader for chaining
	 */
	<T> ConfigReader registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass);

	/**
	 * Get the configured format.
	 *
	 * @return the format
	 */
	Format getFormat();

	/**
	 * Load configuration from a file.
	 * <p>
	 * If the file doesn't exist, creates a new instance with default values
	 * from {@code @Field} annotations.
	 *
	 * @param filePath    path to the config file (without extension if format is configured)
	 * @param configClass the configuration class
	 * @param <T>         the configuration type
	 * @return the loaded configuration
	 * @throws ConfigException if loading fails
	 */
	<T> T load(String filePath, Class<T> configClass);

	/**
	 * Checks if a configuration file exists.
	 *
	 * @param filePath the file path
	 * @return true if the file exists
	 */
	boolean exists(String filePath);
}

