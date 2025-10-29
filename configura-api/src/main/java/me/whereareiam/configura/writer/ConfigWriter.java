package me.whereareiam.configura.writer;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.exception.ConfigException;
// Factory intentionally not present in API; see bootstrap module
import me.whereareiam.configura.type.Format;

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
	 * Register a custom type adapter.
	 *
	 * @param type         the type to adapt
	 * @param adapterClass the adapter class
	 * @param <T>          the type
	 * @return this writer for chaining
	 */
	<T> ConfigWriter registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass);

	/**
	 * Save configuration to a file.
	 * Creates the file if it doesn't exist.
	 *
	 * @param filePath path to the config file (without extension if format is configured)
	 * @param config   the configuration to save
	 * @param <T>      the configuration type
	 * @throws ConfigException if saving fails
	 */
	<T> void save(String filePath, T config);

	/**
	 * Save and immediately read back the configuration.
	 * Ensures the configuration matches what's on disk.
	 *
	 * @param filePath path to the config file
	 * @param config   the configuration to save
	 * @param <T>      the configuration type
	 * @return the configuration read back from disk
	 * @throws ConfigException if saving or reading fails
	 */
	<T> T updateRead(String filePath, T config);

	/**
	 * Get the configured format.
	 *
	 * @return the format
	 */
	Format getFormat();
}

