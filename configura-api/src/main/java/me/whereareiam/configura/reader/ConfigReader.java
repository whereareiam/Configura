package me.whereareiam.configura.reader;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.type.Format;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Reads configuration from files.
 * <p>
 * Use this when you need explicit control over format.
 * Configure once and reuse for multiple files.
 */
public interface ConfigReader {
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
	 * @param file        name of file (without extension if format is configured)
	 * @param configClass the configuration class
	 * @param <T>         the configuration type
	 * @return the loaded configuration
	 * @throws ConfigException if loading fails
	 */
	<T> T load(String file, Class<T> configClass);

	/**
	 * Load configuration from a file path.
	 * <p>
	 * If the file doesn't exist, creates a new instance with default values
	 * from {@code @Field} annotations.
	 *
	 * @param path        the file path to load from
	 * @param configClass the configuration class
	 * @param <T>         the configuration type
	 * @return the loaded configuration
	 * @throws ConfigException if loading fails
	 */
	<T> T load(Path path, Class<T> configClass);

	/**
	 * Deserialize configuration from raw bytes.
	 * <p>
	 * If bytes are null/empty, returns a new instance with default values
	 * from {@code @Field} annotations.
	 *
	 * @param bytes       input data
	 * @param configClass the configuration class
	 * @param <T>         the configuration type
	 * @return the deserialized configuration
	 * @throws ConfigException if deserialization fails
	 */
	<T> T load(byte[] bytes, Class<T> configClass);

	/**
	 * Deserialize configuration from an InputStream.
	 * <p>
	 * If stream is null, returns a new instance with default values
	 * from {@code @Field} annotations.
	 *
	 * <p>Note: The stream is not closed by this method.
	 *
	 * @param inputStream input stream with configuration data
	 * @param configClass the configuration class
	 * @param <T>         the configuration type
	 * @return the deserialized configuration
	 * @throws ConfigException if deserialization fails
	 */
	<T> T load(InputStream inputStream, Class<T> configClass);

	/**
	 * Create a new instance with default values from {@code @Field} annotations.
	 *
	 * @param configClass the configuration class
	 * @param <T>         the configuration type
	 * @return a new instance with default values
	 * @throws ConfigException if instantiation fails
	 */
	<T> T createDefault(Class<T> configClass);
}
