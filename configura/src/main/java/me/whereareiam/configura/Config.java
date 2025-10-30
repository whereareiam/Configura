package me.whereareiam.configura;

import lombok.Getter;
import me.whereareiam.configura.builder.PolymorphicBuilder;
import me.whereareiam.configura.common.polymorphic.PolymorphicRegistry;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.template.DefaultTemplateRegistry;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.InputStream;
import java.nio.file.Path;

@SuppressWarnings("unused")
public final class Config {
	@Getter
	private static ConfigReader defaultReader = new DefaultConfigReader().withFormat(Format.YAML);
	@Getter
	private static ConfigWriter defaultWriter = new DefaultConfigWriter().withFormat(Format.YAML);
	@Getter
	private static TemplateRegistry defaultTemplateRegistry = new DefaultTemplateRegistry();

	/**
	 * Create a new independent reader instance with default settings.
	 */
	public static ConfigReader reader() {
		return new DefaultConfigReader().withFormat(Format.YAML);
	}

	/**
	 * Create a new independent reader instance with the given format.
	 */
	public static ConfigReader reader(Format format) {
		return new DefaultConfigReader().withFormat(format);
	}

	/**
	 * Create a new independent writer instance with default settings.
	 */
	public static ConfigWriter writer() {
		return new DefaultConfigWriter().withFormat(Format.YAML);
	}

	/**
	 * Create a new independent writer instance with the given format.
	 */
	public static ConfigWriter writer(Format format) {
		return new DefaultConfigWriter().withFormat(format);
	}

	/**
	 * Create a new template registry instance with default resolver.
	 */
	public static TemplateRegistry templateRegistry() {
		return new DefaultTemplateRegistry();
	}

	public static void setReader(ConfigReader reader) {
		Config.defaultReader = reader;
	}

	public static void setWriter(ConfigWriter writer) {
		Config.defaultWriter = writer;
	}

	public static void setTemplateRegistry(TemplateRegistry registry) {
		Config.defaultTemplateRegistry = registry;
	}

	public static <T> void registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		defaultReader = defaultReader.registerAdapter(type, adapterClass);
		defaultWriter = defaultWriter.registerAdapter(type, adapterClass);
	}

	public static <T> PolymorphicBuilder<T> registerPolymorphic(Class<T> baseType) {
		return PolymorphicRegistry.register(baseType);
	}

	public static <T, P extends TemplateProvider<T>> void registerTemplate(Class<P> providerClass) {
		defaultTemplateRegistry.registerTemplate(providerClass);
	}

	public static <T> T load(String file, Class<T> configClass) {
		return getDefaultReader().load(file, configClass);
	}

	public static <T> T load(Path path, Class<T> configClass) {
		return getDefaultReader().load(path, configClass);
	}

	public static <T> T load(byte[] bytes, Class<T> configClass) {
		return getDefaultReader().load(bytes, configClass);
	}

	public static <T> T load(InputStream inputStream, Class<T> configClass) {
		return getDefaultReader().load(inputStream, configClass);
	}

	public static <T> void save(String file, T config) {
		getDefaultWriter().save(file, config);
	}

	public static <T> void save(Path path, T config) {
		getDefaultWriter().save(path, config);
	}

	public static <T> byte[] save(T config) {
		return getDefaultWriter().save(config);
	}

	public static <T> T merge(String file, T config) {
		return getDefaultWriter().merge(file, config);
	}

	public static <T> T merge(Path path, T config) {
		return getDefaultWriter().merge(path, config);
	}

	/**
	 * Creates a default instance of the config class, saves it to file, then reloads it.
	 * <p>
	 * Useful for ensuring configuration files exist with default values.
	 *
	 * @param file        name of file (without extension if format is configured)
	 * @param configClass the configuration class
	 * @param <T>         the configuration type
	 * @return the loaded configuration
	 */
	public static <T> T update(String file, Class<T> configClass) {
		T defaultInstance = getDefaultReader().createDefault(configClass);
		getDefaultWriter().save(file, defaultInstance);
		return getDefaultReader().load(file, configClass);
	}

	/**
	 * Creates a default instance of the config class, saves it to file, then reloads it.
	 * <p>
	 * Useful for ensuring configuration files exist with default values.
	 *
	 * @param path        full path to the config file (directory + filename)
	 * @param configClass the configuration class
	 * @param <T>         the configuration type
	 * @return the loaded configuration
	 */
	public static <T> T update(Path path, Class<T> configClass) {
		T defaultInstance = getDefaultReader().createDefault(configClass);
		getDefaultWriter().save(path, defaultInstance);
		return getDefaultReader().load(path, configClass);
	}
}