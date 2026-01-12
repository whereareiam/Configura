package me.whereareiam.configura.reader;

import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Reads configuration from files.
 * <p>
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
     * Configure the template registry used when constructing default instances during decoding.
     * No templates are applied on file reads.
     *
     * @param templateRegistry registry that maps model types to {@code TemplateProvider}s
     * @return this reader for chaining
     */
	ConfigReader withTemplateRegistry(TemplateRegistry templateRegistry);

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
	 * Register a custom type adapter instance.
	 * This allows for dependency injection into adapters.
	 *
	 * @param type            the type to adapt
	 * @param adapterInstance the adapter instance
	 * @param <T>             the type
	 * @return this reader for chaining
	 */
	<T> ConfigReader registerAdapter(Class<T> type, TypeAdapter<T> adapterInstance);

    /**
     * Get the configured file format (YAML/JSON).
     *
     * @return the configured format
     */
	Format getFormat();

	/**
	 * Deserialize raw bytes into a Configura node tree.
	 *
	 * @param bytes input data
	 * @return parsed node tree
	 * @throws ConfigException if deserialization fails
	 */
	Node decodeNode(byte[] bytes);

	/**
	 * Deserialize an {@link InputStream} into a Configura node tree.
	 * Note: the stream is not closed by this method.
	 *
	 * @param inputStream input data stream
	 * @return parsed node tree
	 * @throws ConfigException if deserialization fails
	 */
	Node decodeNode(InputStream inputStream);

	/**
	 * Strict read of a node tree from a file path (string).
	 *
	 * @param file name of file (without extension if format is configured)
	 * @return parsed node tree
	 * @throws ConfigException if loading fails
	 */
	Node readNode(String file);

	/**
	 * Strict read of a node tree from a file path.
	 *
	 * @param path file path to read from
	 * @return parsed node tree
	 * @throws ConfigException if loading fails
	 */
	Node readNode(Path path);

    /**
     * Pure deserialization from raw bytes. If {@code bytes} are null/empty, returns a new instance
     * with default values defined by the model (constructors/initializers/Jackson defaults).
     *
     * @param bytes       input data
     * @param configClass the configuration class
     * @param <T>         the configuration type
     * @return the deserialized configuration
     * @throws ConfigException if deserialization fails
     */
	<T> T decode(byte[] bytes, Class<T> configClass);

    /**
     * Pure deserialization from an {@link InputStream}. If {@code inputStream} is null, returns a
     * new instance with default values defined by the model (constructors/initializers/Jackson defaults).
     * Note: the stream is not closed by this method.
     *
     * @param inputStream input stream with configuration data
     * @param configClass the configuration class
     * @param <T>         the configuration type
     * @return the deserialized configuration
     * @throws ConfigException if deserialization fails
     */
	<T> T decode(InputStream inputStream, Class<T> configClass);

    /**
     * Strict read from a file path (string). Fails if the file is missing or invalid.
     * No templates are applied on read.
     *
     * @param file        name of file (without extension if format is configured)
     * @param configClass the configuration class
     * @param <T>         the configuration type
     * @return the loaded configuration
     * @throws ConfigException if loading fails
     */
	<T> T read(String file, Class<T> configClass);

    /**
     * Strict read from a file path. Fails if the file is missing or invalid.
     * No templates are applied on read.
     *
     * @param path        the file path to read from
     * @param configClass the configuration class
     * @param <T>         the configuration type
     * @return the loaded configuration
     * @throws ConfigException if loading fails
     */
	<T> T read(Path path, Class<T> configClass);

    /**
     * Load configuration from a file (legacy convenience).
     * <p>
     * Resolves the file name using {@link Format} when the extension is omitted and then delegates to
     * {@link #read(Path, Class)}.
     *
     * @param file        name of file (without extension if format is configured)
     * @param configClass the configuration class
     * @param <T>         the configuration type
     * @return the loaded configuration
     * @throws ConfigException if loading fails
     */
	<T> T load(String file, Class<T> configClass);

    /**
     * Load configuration from a file path (legacy convenience).
     * <p>
     * Delegates to {@link #read(Path, Class)}.
     *
     * @param path        the file path to load from
     * @param configClass the configuration class
     * @param <T>         the configuration type
     * @return the loaded configuration
     * @throws ConfigException if loading fails
     */
	<T> T load(Path path, Class<T> configClass);

    /**
     * Deserialize configuration from raw bytes (legacy convenience).
     * <p>
     * Delegates to {@link #decode(byte[], Class)}.
     *
     * @param bytes       input data
     * @param configClass the configuration class
     * @param <T>         the configuration type
     * @return the deserialized configuration
     * @throws ConfigException if deserialization fails
     */
	<T> T load(byte[] bytes, Class<T> configClass);

    /**
     * Deserialize configuration from an {@link InputStream} (legacy convenience).
     * <p>
     * Delegates to {@link #decode(InputStream, Class)}. Note: the stream is not closed by this method.
     *
     * @param inputStream input stream with configuration data
     * @param configClass the configuration class
     * @param <T>         the configuration type
     * @return the deserialized configuration
     * @throws ConfigException if deserialization fails
     */
	<T> T load(InputStream inputStream, Class<T> configClass);

	/**
	 * Convenience load of a node tree from a file path (string).
	 *
	 * @param file name of file (without extension if format is configured)
	 * @return parsed node tree
	 */
	default Node loadNode(String file) {
		return readNode(file);
	}

	/**
	 * Convenience load of a node tree from a file path.
	 *
	 * @param path file path to read from
	 * @return parsed node tree
	 */
	default Node loadNode(Path path) {
		return readNode(path);
	}

	/**
	 * Convenience load of a node tree from raw bytes.
	 *
	 * @param bytes input data
	 * @return parsed node tree
	 */
	default Node loadNode(byte[] bytes) {
		return decodeNode(bytes);
	}

	/**
	 * Convenience load of a node tree from an input stream.
	 *
	 * @param inputStream input data stream
	 * @return parsed node tree
	 */
	default Node loadNode(InputStream inputStream) {
		return decodeNode(inputStream);
	}
}
