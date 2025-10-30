package me.whereareiam.configura.writer;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.template.TemplateRegistry;

import java.nio.file.Path;

/**
 * Writes configuration to files.
 * <p>
 * Use this when you need explicit control over format.
 * Configure once and reuse for multiple files.
 */
public interface ConfigWriter {
	/**
	 * Configure the file format to use.
	 *
	 * @param format the format (YAML or JSON)
	 * @return this writer for chaining
	 */
	ConfigWriter withFormat(Format format);

    /**
     * Configure the template registry to use for default seeding.
     */
    ConfigWriter withTemplateRegistry(TemplateRegistry templateRegistry);

	/**
	 * Register a custom type adapter.
	 *
	 * @param type         the type to adapt
	 * @param adapterClass the adapter class
	 * @param <T>          the type
	 * @return this writer for chaining
	 */
	<T> ConfigWriter registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass);

	/**
	 * Get the configured format.
	 *
	 * @return the format
	 */
	Format getFormat();

	/**
	 * Save configuration to a file.
	 * Creates the file if it doesn't exist.
	 *
	 * @param file   path to the config file (without extension if format is configured)
	 * @param config the configuration to save
	 * @param <T>    the configuration type
	 * @throws ConfigException if saving fails
	 */
	<T> void save(String file, T config);

	/**
	 * Save configuration to an explicit file path.
	 * Creates the file if it doesn't exist.
	 * <p>
	 * If the provided path does not include an extension, an extension matching the configured
	 * {@link Format} will be appended.
	 *
	 * @param path   full path to the config file (directory + filename)
	 * @param config the configuration to save
	 * @param <T>    the configuration type
	 * @throws ConfigException if saving fails
	 */
	<T> void save(Path path, T config);

	/**
	 * Merge existing file content (if present) with the provided model, honoring
	 * {@code @Policy(mergeOnUpdate = false)} on fields to preserve existing values.
	 * Does not write the file.
	 *
	 * @param file   path to the config file (without extension if format is configured)
	 * @param config the incoming model to merge
	 * @param <T>    the configuration type
	 * @return merged configuration instance
	 */
	<T> T merge(String file, T config);

	/**
	 * Merge existing file content (if present) with the provided model, honoring
	 * {@code @Policy(mergeOnUpdate = false)} on fields to preserve existing values.
	 * Does not write the file.
	 * <p>
	 * If the provided path does not include an extension, an extension matching the configured
	 * {@link Format} will be considered when resolving the path.
	 *
	 * @param path   full path to the config file (directory + filename)
	 * @param config the incoming model to merge
	 * @param <T>    the configuration type
	 * @return merged configuration instance
	 */
	<T> T merge(Path path, T config);

	/**
	 * Serialize configuration to raw bytes using the configured format.
	 *
	 * @param config the configuration to serialize
	 * @param <T>    the configuration type
	 * @return serialized bytes
	 * @throws ConfigException if serialization fails
	 */
	<T> byte[] save(T config);
}

