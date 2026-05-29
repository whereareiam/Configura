package me.whereareiam.configura.reader;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.exception.ConfigException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Reads configuration from files, bytes, and streams.
 */
public interface ConfigReader {
	/**
	 * Strict read from a file path. Fails if the file is missing or invalid.
	 *
	 * @param path the file path to read from
	 * @param configClass the configuration class
	 * @param <T> the configuration type
	 * @return the loaded configuration
	 * @throws ConfigException if loading fails
	 */
	<T> T read(Path path, Class<T> configClass);

	/**
	 * Deserialize configuration from raw bytes. If {@code bytes} are null or empty, returns a new
	 * instance with default values defined by the model.
	 *
	 * @param bytes input data
	 * @param configClass the configuration class
	 * @param <T> the configuration type
	 * @return the deserialized configuration
	 * @throws ConfigException if deserialization fails
	 */
	<T> T read(byte[] bytes, Class<T> configClass);

	/**
	 * Deserialize configuration from an {@link InputStream}. If {@code inputStream} is null, returns
	 * a new instance with default values defined by the model.
	 *
	 * @param inputStream input stream with configuration data
	 * @param configClass the configuration class
	 * @param <T> the configuration type
	 * @return the deserialized configuration
	 * @throws ConfigException if deserialization fails
	 */
	<T> T read(InputStream inputStream, Class<T> configClass);

	/**
	 * Read a raw configuration tree from a file path. Fails if the file is missing or invalid.
	 *
	 * @param path the file path to read from
	 * @return the parsed configuration tree
	 * @throws ConfigException if reading fails
	 */
	JsonNode readNode(Path path);

	/**
	 * Read a raw configuration tree from bytes.
	 * If {@code bytes} are null or empty, returns an empty object node.
	 *
	 * @param bytes input data
	 * @return the parsed configuration tree
	 * @throws ConfigException if reading fails
	 */
	JsonNode readNode(byte[] bytes);

	/**
	 * Read a raw configuration tree from an {@link InputStream}.
	 * If {@code inputStream} is null, returns an empty object node.
	 *
	 * @param inputStream input stream with configuration data
	 * @return the parsed configuration tree
	 * @throws ConfigException if reading fails
	 */
	JsonNode readNode(InputStream inputStream);
}
