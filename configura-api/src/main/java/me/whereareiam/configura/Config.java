package me.whereareiam.configura;

import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Static helper for loading and saving configurations.
 * <p>
 * This is the simplest way to use Configura. Configure default readers/writers
 * or use the extension-based auto-detection.
 *
 * <p>Quick start:
 * <pre>{@code
 * // Save with auto-detected format from extension
 * AppConfig cfg = new AppConfig();
 * Config.save("app.yaml", cfg);
 *
 * // Load with auto-detected format
 * AppConfig cfg2 = Config.load("app.yaml", AppConfig.class);
 *
 * // Register a custom type adapter
 * Config.registerAdapter(Duration.class, DurationAdapter.class);
 *
 * // Initialize with a custom service implementation (optional)
 * Config.init(myService);
 * }</pre>
 */
@SuppressWarnings("unused")
public final class Config {
	private static ConfigReader defaultReader;
	private static ConfigWriter defaultWriter;
	private static volatile ConfigService service;

	/**
	 * Configure the default reader used by static methods.
	 *
	 * @param reader the reader to use as default
	 */
	public static void setDefaultReader(ConfigReader reader) {
		Config.defaultReader = reader;
	}

	/**
	 * Configure the default writer used by static methods.
	 *
	 * @param writer the writer to use as default
	 */
	public static void setDefaultWriter(ConfigWriter writer) {
		Config.defaultWriter = writer;
	}

	/**
	 * Initialize the underlying service used by this facade.
	 * If not set, a service will be discovered via ServiceLoader on first use.
	 */
	public static void init(ConfigService svc) {
		service = Objects.requireNonNull(svc);
	}

	/**
	 * Register a type adapter globally.
	 * Works for both reading and writing.
	 *
	 * @param type         the type to adapt
	 * @param adapterClass the adapter class
	 * @param <T>          the type
	 */
	public static <T> void registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		ConfigService s = getServiceOrNull();
		if (s != null) {
			service = s.withAdapter(type, adapterClass);
			return;
		}

		getDefaultReader().registerAdapter(type, adapterClass);
		getDefaultWriter().registerAdapter(type, adapterClass);
	}

	/**
	 * Load configuration from a file.
	 * Auto-detects format from file extension (.yaml/.yml → YAML, .json → JSON).
	 *
	 * @param filePath    the file path with extension
	 * @param configClass the configuration class
	 * @param <T>         the configuration type
	 * @return the loaded configuration
	 * @throws ConfigException if loading fails
	 */
	public static <T> T load(String filePath, Class<T> configClass) {
		ConfigService s = getServiceOrNull();
		if (s != null) return s.load(filePath, configClass);

		return getReaderForFile(filePath).load(filePath, configClass);
	}

	/**
	 * Save configuration to a file.
	 * Auto-detects format from file extension (.yaml/.yml → YAML, .json → JSON).
	 *
	 * @param filePath the file path with extension
	 * @param config   the configuration to save
	 * @param <T>      the configuration type
	 * @throws ConfigException if saving fails
	 */
	public static <T> void save(String filePath, T config) {
		ConfigService s = getServiceOrNull();
		if (s != null) {
			s.save(filePath, config);
			return;
		}

		getWriterForFile(filePath).save(filePath, config);
	}

	/**
	 * Save and immediately read back the configuration.
	 * Ensures the configuration matches what's on disk.
	 *
	 * @param filePath the file path with extension
	 * @param config   the configuration to save
	 * @param <T>      the configuration type
	 * @return the configuration read back from disk
	 * @throws ConfigException if saving or reading fails
	 */
	public static <T> T updateRead(String filePath, T config) {
		ConfigService s = getServiceOrNull();
		if (s != null) return s.updateRead(filePath, config);

		return getWriterForFile(filePath).updateRead(filePath, config);
	}

	/**
	 * Gets the default reader instance.
	 * Used internally but also available for external configuration.
	 *
	 * @return the default reader
	 */
	public static ConfigReader getDefaultReader() {
		if (defaultReader == null) defaultReader = ConfigReader.create().withFormat(Format.YAML);

		return defaultReader;
	}

	private static ConfigWriter getDefaultWriter() {
		if (defaultWriter == null) defaultWriter = ConfigWriter.create().withFormat(Format.YAML);

		return defaultWriter;
	}

	private static ConfigService getServiceOrNull() {
		ConfigService s = service;
		if (s != null) return s;

		ServiceLoader<ConfigService> loader = ServiceLoader.load(ConfigService.class);
		for (ConfigService impl : loader) {
			service = impl;
			return impl;
		}

		return null;
	}

	private static ConfigReader getReaderForFile(String filePath) {
		Format detectedFormat = detectFormat(filePath);
		if (defaultReader != null && defaultReader.getFormat() == detectedFormat)
			return defaultReader;

		return ConfigReader.create().withFormat(detectedFormat);
	}

	private static ConfigWriter getWriterForFile(String filePath) {
		Format detectedFormat = detectFormat(filePath);
		if (defaultWriter != null && defaultWriter.getFormat() == detectedFormat)
			return defaultWriter;

		return ConfigWriter.create().withFormat(detectedFormat);
	}

	private static Format detectFormat(String filePath) {
		String lower = filePath.toLowerCase();
		if (lower.endsWith(".yaml") || lower.endsWith(".yml"))
			return Format.YAML;

		if (lower.endsWith(".json"))
			return Format.JSON;

		// Default to YAML
		return Format.YAML;
	}
}
