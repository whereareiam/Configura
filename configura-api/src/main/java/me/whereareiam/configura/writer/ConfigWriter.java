package me.whereareiam.configura.writer;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.exception.ConfigException;
import java.nio.file.Path;

/**
 * Writes configuration to files and bytes.
 */
public interface ConfigWriter {
	/**
	 * Exact write to an explicit path.
	 *
	 * @param path full path to the config file (directory + filename)
	 * @param config the configuration to write as-is
	 * @param <T> the configuration type
	 * @throws ConfigException if writing fails
	 */
	<T> void write(Path path, T config);

	/**
	 * Serialize configuration to raw bytes.
	 *
	 * @param config the configuration to write as-is
	 * @param <T> the configuration type
	 * @return serialized bytes
	 * @throws ConfigException if serialization fails
	 */
	<T> byte[] writeBytes(T config);

	/**
	 * Write a raw configuration tree to an explicit path.
	 *
	 * @param path full path to the config file (directory + filename)
	 * @param node the raw configuration tree to write
	 * @throws ConfigException if writing fails
	 */
	void writeNode(Path path, JsonNode node);

	/**
	 * Serialize a raw configuration tree to bytes.
	 *
	 * @param node the raw configuration tree to serialize
	 * @return serialized bytes
	 * @throws ConfigException if serialization fails
	 */
	byte[] writeNodeBytes(JsonNode node);
}
