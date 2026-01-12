package me.whereareiam.configura;

import lombok.Getter;
import me.whereareiam.configura.builder.PolymorphicBuilder;
import me.whereareiam.configura.common.polymorphic.PolymorphicRegistry;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.template.DefaultTemplateRegistry;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.InputStream;
import java.nio.file.Path;

@SuppressWarnings("unused")
public final class Config {
	@Getter
	private static TemplateRegistry defaultTemplateRegistry = new DefaultTemplateRegistry();
	@Getter
	private static ConfigReader defaultReader = new DefaultConfigReader().withTemplateRegistry(defaultTemplateRegistry).withFormat(Format.YAML);
	@Getter
	private static ConfigWriter defaultWriter = new DefaultConfigWriter().withTemplateRegistry(defaultTemplateRegistry).withFormat(Format.YAML);

	/**
	 * Create a new independent reader instance with default settings.
	 */
	public static ConfigReader reader() {
		return new DefaultConfigReader().withTemplateRegistry(defaultTemplateRegistry).withFormat(Format.YAML);
	}

	/**
	 * Create a new independent reader instance with the given format.
	 */
	public static ConfigReader reader(Format format) {
		return new DefaultConfigReader().withTemplateRegistry(defaultTemplateRegistry).withFormat(format);
	}

	/**
	 * Create a new independent writer instance with default settings.
	 */
	public static ConfigWriter writer() {
		return new DefaultConfigWriter().withTemplateRegistry(defaultTemplateRegistry).withFormat(Format.YAML);
	}

	/**
	 * Create a new independent writer instance with the given format.
	 */
	public static ConfigWriter writer(Format format) {
		return new DefaultConfigWriter().withTemplateRegistry(defaultTemplateRegistry).withFormat(format);
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
		Config.defaultReader = new DefaultConfigReader().withTemplateRegistry(defaultTemplateRegistry).withFormat(Config.defaultReader.getFormat());
		Config.defaultWriter = new DefaultConfigWriter().withTemplateRegistry(defaultTemplateRegistry).withFormat(Config.defaultWriter.getFormat());
	}

	public static <T> void registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		defaultReader = defaultReader.registerAdapter(type, adapterClass);
		defaultWriter = defaultWriter.registerAdapter(type, adapterClass);
	}

	/**
	 * Register a type adapter instance for the given type.
	 * This allows for dependency injection into adapters.
	 *
	 * @param type the type to register an adapter for
	 * @param adapterInstance the adapter instance
	 * @param <T> the type
	 */
	public static <T> void registerAdapter(Class<T> type, TypeAdapter<T> adapterInstance) {
		defaultReader = defaultReader.registerAdapter(type, adapterInstance);
		defaultWriter = defaultWriter.registerAdapter(type, adapterInstance);
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

	public static Node loadNode(String file) {
		return getDefaultReader().loadNode(file);
	}

	public static Node loadNode(Path path) {
		return getDefaultReader().loadNode(path);
	}

	public static Node loadNode(byte[] bytes) {
		return getDefaultReader().loadNode(bytes);
	}

	public static Node loadNode(InputStream inputStream) {
		return getDefaultReader().loadNode(inputStream);
	}

	public static <T> void save(String file, T config) {
		getDefaultWriter().encode(file, config);
	}

	public static <T> void save(Path path, T config) {
		getDefaultWriter().encode(path, config);
	}

	public static void saveNode(String file, Node node) {
		getDefaultWriter().writeNode(file, node);
	}

	public static void saveNode(Path path, Node node) {
		getDefaultWriter().writeNode(path, node);
	}

	public static byte[] saveNode(Node node) {
		return getDefaultWriter().encodeNode(node);
	}

	public static <T> byte[] save(T config) {
		return getDefaultWriter().encode(config);
	}

	public static <T> T merge(String file, T config) {
		return getDefaultWriter().merge(file, config);
	}

	public static <T> T merge(Path path, T config) {
		return getDefaultWriter().merge(path, config);
	}

	public static <T> T update(String file, Class<T> configClass) {
		T defaultInstance = getDefaultReader().load(new byte[0], configClass);
		T merged = getDefaultWriter().merge(file, defaultInstance);
		getDefaultWriter().write(file, merged);
		return getDefaultReader().load(file, configClass);
	}

	public static <T> T update(Path path, Class<T> configClass) {
		T defaultInstance = getDefaultReader().load(new byte[0], configClass);
		T merged = getDefaultWriter().merge(path, defaultInstance);
		getDefaultWriter().write(path, merged);
		return getDefaultReader().load(path, configClass);
	}
}
