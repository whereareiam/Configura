package me.whereareiam.configura.writer;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;

import java.nio.file.Path;

/**
 * Writes configuration to files.
 * <p>
 * Configure once and reuse for multiple files. The writer supports both "smart" writes (that apply
 * templates and honor per-field merge policies) and "exact" writes that simply overwrite the file.
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
	 * Configure the template registry used to seed default values during smart writes.
	 *
	 * @param templateRegistry registry that maps model types to {@code TemplateProvider}s
	 * @return this writer for chaining
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
	 * Register a custom type adapter instance.
	 * This allows for dependency injection into adapters.
	 *
	 * @param type            the type to adapt
	 * @param adapterInstance the adapter instance
	 * @param <T>             the type
	 * @return this writer for chaining
	 */
	<T> ConfigWriter registerAdapter(Class<T> type, TypeAdapter<T> adapterInstance);

	/**
	 * Get the configured file format for serialization (YAML/JSON).
	 *
	 * @return the configured format
	 */
	Format getFormat();

	/**
	 * Smart write (apply) to a file path (string).
	 *
	 * <p>Behavior:
	 * <ul>
	 *   <li>Seeds missing fields using registered templates</li>
	 *   <li>Preserves existing values for fields annotated with {@code @Policy(mergeOnUpdate = false)}</li>
	 *   <li>Creates parent directories and the file if they do not exist</li>
	 *   <li>If {@code file} omits an extension, an extension matching {@link Format} is appended</li>
	 * </ul>
	 *
	 * @param file   path to the config file (without extension if format is configured)
	 * @param config the configuration to save
	 * @param <T>    the configuration type
	 * @throws ConfigException if saving fails
	 */
	<T> void encode(String file, T config);

	/**
	 * Smart write (apply) to an explicit path.
	 *
	 * <p>Behavior:
	 * <ul>
	 *   <li>Seeds missing fields using registered templates</li>
	 *   <li>Preserves existing values for fields annotated with {@code @Policy(mergeOnUpdate = false)}</li>
	 *   <li>Creates parent directories and the file if they do not exist</li>
	 *   <li>If {@code path} omits an extension, an extension matching {@link Format} is appended</li>
	 * </ul>
	 *
	 * @param path   full path to the config file (directory + filename)
	 * @param config the configuration to save
	 * @param <T>    the configuration type
	 * @throws ConfigException if saving fails
	 */
	<T> void encode(Path path, T config);

	/**
	 * Serialize configuration to raw bytes using the configured format.
	 *
	 * @param config the configuration to serialize
	 * @param <T>    the configuration type
	 * @return serialized bytes
	 * @throws ConfigException if serialization fails
	 */
	<T> byte[] encode(T config);

	/**
	 * Exact write (no seeding, no policy preservation) to an explicit path.
	 * Creates parent directories if needed. If {@code path} omits an extension, one matching
	 * {@link Format} is appended.
	 *
	 * @param path   full path to the config file (directory + filename)
	 * @param config the configuration to write as-is
	 * @param <T>    the configuration type
	 * @throws ConfigException if writing fails
	 */
	<T> void write(Path path, T config);

	/**
	 * Exact write (no seeding, no policy preservation) to a file path (string).
	 * Creates parent directories if needed. If {@code file} omits an extension, one matching
	 * {@link Format} is appended.
	 *
	 * @param file   path to the config file (without extension if format is configured)
	 * @param config the configuration to write as-is
	 * @param <T>    the configuration type
	 * @throws ConfigException if writing fails
	 */
	<T> void write(String file, T config);

	/**
	 * Compute the smart-apply result (seed + policy) for the given file path (string) without writing.
	 *
	 * @param file   path to the config file (without extension if format is configured)
	 * @param config the incoming model to merge with existing file content
	 * @param <T>    the configuration type
	 * @return merged configuration instance
	 */
	<T> T merge(String file, T config);


	/**
	 * Compute the smart-apply result (seed + policy) for the given path without writing.
	 * If the path omits an extension, an extension matching the configured {@link Format} is appended.
	 *
	 * @param path   full path to the config file (directory + filename)
	 * @param config the incoming model to merge with existing file content
	 * @param <T>    the configuration type
	 * @return merged configuration instance
	 */
	<T> T merge(Path path, T config);

	/**
	 * Serialize a Configura node tree to raw bytes using the configured format.
	 *
	 * @param node node tree to serialize
	 * @return serialized bytes
	 * @throws ConfigException if serialization fails
	 */
	byte[] encodeNode(Node node);

	/**
	 * Exact write of a Configura node tree to a file path (string).
	 * Creates parent directories if needed. If {@code file} omits an extension, one matching
	 * {@link Format} is appended.
	 *
	 * @param file path to the config file (without extension if format is configured)
	 * @param node node tree to write
	 * @throws ConfigException if writing fails
	 */
	void writeNode(String file, Node node);

	/**
	 * Exact write of a Configura node tree to a file path.
	 * Creates parent directories if needed. If {@code path} omits an extension, one matching
	 * {@link Format} is appended.
	 *
	 * @param path full path to the config file (directory + filename)
	 * @param node node tree to write
	 * @throws ConfigException if writing fails
	 */
	void writeNode(Path path, Node node);
}

