package me.whereareiam.configura;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import me.whereareiam.configura.builder.PolymorphicBuilder;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.polymorphic.PolymorphicRegistry;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.Configura;
import me.whereareiam.configura.common.template.DefaultTemplateRegistry;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.merge.MergePolicy;
import me.whereareiam.configura.merge.MergePolicyRegistry;
import me.whereareiam.configura.type.MergePreset;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Config {
	@Getter
	private static TemplateRegistry defaultTemplateRegistry = new DefaultTemplateRegistry();
	@Getter
	private static ConfigReader defaultReader = new DefaultConfigReader().withTemplateRegistry(defaultTemplateRegistry).withFormat(Format.YAML);
	@Getter
	private static ConfigWriter defaultWriter = new DefaultConfigWriter().withTemplateRegistry(defaultTemplateRegistry).withFormat(Format.YAML);
	@Getter
	private static Config defaultConfig = builder().format(Format.YAML).build();

	private final Configura runtime;
	private final Objects objects = new Objects();
	private final Trees trees = new Trees();

	private Config(Configura runtime) {
		this.runtime = runtime;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Convenience config configured for YAML.
	 */
	public static Config yaml() {
		return builder().format(Format.YAML).build();
	}

	/**
	 * Convenience config configured for JSON.
	 */
	public static Config json() {
		return builder().format(Format.JSON).build();
	}

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
		rebuildDefaultConfig();
	}

	public static void setWriter(ConfigWriter writer) {
		Config.defaultWriter = writer;
		rebuildDefaultConfig();
	}

	public static void setTemplateRegistry(TemplateRegistry registry) {
		Config.defaultTemplateRegistry = registry;
		Config.defaultReader = new DefaultConfigReader().withTemplateRegistry(defaultTemplateRegistry).withFormat(Config.defaultReader.getFormat());
		Config.defaultWriter = new DefaultConfigWriter().withTemplateRegistry(defaultTemplateRegistry).withFormat(Config.defaultWriter.getFormat());
		rebuildDefaultConfig();
	}

	public static <T> PolymorphicBuilder<T> registerPolymorphic(Class<T> baseType) {
		return PolymorphicRegistry.register(baseType);
	}

	public static <T, P extends TemplateProvider<T>> void registerTemplate(Class<P> providerClass) {
		defaultTemplateRegistry.registerTemplate(providerClass);
		rebuildDefaultConfig();
	}

	public static <T> T load(String file, Class<T> configClass) {
		return getDefaultConfig().objects().read(file, configClass);
	}

	public static <T> T load(Path path, Class<T> configClass) {
		return getDefaultConfig().objects().read(path, configClass);
	}

	public static <T> T load(byte[] bytes, Class<T> configClass) {
		return getDefaultConfig().objects().read(bytes, configClass);
	}

	public static <T> T load(InputStream inputStream, Class<T> configClass) {
		return getDefaultConfig().objects().read(inputStream, configClass);
	}

	public static <T> void save(String file, T config) {
		getDefaultConfig().objects().save(file, config);
	}

	public static <T> void save(Path path, T config) {
		getDefaultConfig().objects().save(path, config);
	}

	public static <T> byte[] save(T config) {
		return getDefaultConfig().objects().writeBytes(config);
	}

	public static <T> T merge(String file, T config) {
		return getDefaultConfig().objects().merge(file, config);
	}

	public static <T> T merge(Path path, T config) {
		return getDefaultConfig().objects().merge(path, config);
	}

	public static <T> T update(String file, Class<T> configClass) {
		return getDefaultConfig().objects().update(file, configClass);
	}

	public static <T> T update(Path path, Class<T> configClass) {
		return getDefaultConfig().objects().update(path, configClass);
	}

	private static void rebuildDefaultConfig() {
		defaultConfig = builder().format(defaultReader.getFormat()).build();
	}

	public ObjectMapper mapper() {
		return runtime.mapper();
	}

	public Config withModule(Module module) {
		return new Config(runtime.withModule(module));
	}

	public <T, P extends TemplateProvider<T>> Config withTemplate(Class<P> providerClass) {
		return new Config(runtime.withTemplate(providerClass));
	}

	public Config withMergePolicy(String name, MergePolicy policy) {
		return new Config(runtime.withMergePolicy(name, policy));
	}

	public Config withDefaultMergePreset(MergePreset preset) {
		return new Config(runtime.withDefaultMergePolicy(preset.policy()));
	}

	public Config withDefaultMergePolicy(MergePolicy policy) {
		return new Config(runtime.withDefaultMergePolicy(policy));
	}

	public Objects objects() {
		return objects;
	}

	public Trees trees() {
		return trees;
	}

	public String extension() {
		return runtime.extension();
	}

	public List<Module> modules() {
		return runtime.modules();
	}

	public Map<String, MergePolicy> mergePolicies() {
		return runtime.policies();
	}

	public MergePolicy defaultMergePolicy() {
		return runtime.defaultPolicy();
	}

	public final class Objects {
		private Objects() {
		}

		public <T> T read(String file, Class<T> type) {
			return runtime.read(file, type);
		}

		public <T> T read(Path path, Class<T> type) {
			return runtime.read(path, type);
		}

		public <T> T read(byte[] bytes, Class<T> type) {
			return runtime.read(bytes, type);
		}

		public <T> T read(InputStream inputStream, Class<T> type) {
			return runtime.read(inputStream, type);
		}

		public <T> void write(String file, T value) {
			runtime.write(file, value);
		}

		public <T> void write(Path path, T value) {
			runtime.write(path, value);
		}

		public <T> void save(String file, T value) {
			runtime.save(file, value);
		}

		public <T> void save(Path path, T value) {
			runtime.save(path, value);
		}

		public <T> byte[] writeBytes(T value) {
			return runtime.writeBytes(value);
		}

		public <T> T merge(String file, T value) {
			return runtime.merge(file, value);
		}

		public <T> T merge(Path path, T value) {
			return runtime.merge(path, value);
		}

		public <T> T update(String file, Class<T> type) {
			return runtime.update(file, type);
		}

		public <T> T update(Path path, Class<T> type) {
			return runtime.update(path, type);
		}
	}

	public final class Trees {
		private Trees() {
		}

		public com.fasterxml.jackson.databind.JsonNode read(String file) {
			return runtime.readTree(file);
		}

		public com.fasterxml.jackson.databind.JsonNode read(Path path) {
			return runtime.readTree(path);
		}

		public com.fasterxml.jackson.databind.JsonNode read(byte[] bytes) {
			return runtime.readTree(bytes);
		}

		public com.fasterxml.jackson.databind.JsonNode read(InputStream inputStream) {
			return runtime.readTree(inputStream);
		}

		public void write(String file, com.fasterxml.jackson.databind.JsonNode node) {
			runtime.writeTree(file, node);
		}

		public void write(Path path, com.fasterxml.jackson.databind.JsonNode node) {
			runtime.writeTree(path, node);
		}

		public byte[] writeBytes(com.fasterxml.jackson.databind.JsonNode node) {
			return runtime.writeTreeBytes(node);
		}
	}

	public static final class Builder {
		private String extension = Format.YAML.getExtension();
		private Function<List<Module>, ObjectMapper> mapperFactory = MapperFactory::createYamlMapper;
		private final List<Module> modules = new ArrayList<>();
		private final DefaultTemplateRegistry templateRegistry = defaultTemplateRegistry instanceof DefaultTemplateRegistry registry
				? registry.copy()
				: new DefaultTemplateRegistry();
		private final MergePolicyRegistry policyRegistry = MergePolicyRegistry.standard();
		private MergePolicy defaultMergePolicy = MergePreset.DEEP_DEFAULTS.policy();

		public Builder format(Format format) {
			if (format == Format.JSON) {
				this.extension = format.getExtension();
				this.mapperFactory = MapperFactory::createJsonMapper;
				return this;
			}

			this.extension = format.getExtension();
			this.mapperFactory = MapperFactory::createYamlMapper;
			return this;
		}

		public Builder format(String extension, Function<List<Module>, ObjectMapper> mapperFactory) {
			this.extension = extension;
			this.mapperFactory = mapperFactory;
			return this;
		}

		public Builder module(Module module) {
			if (module != null)
				this.modules.add(module);
			return this;
		}

		public Builder modules(Module... modules) {
			if (modules == null) return this;
			for (Module module : modules)
				module(module);
			return this;
		}

		public <T, P extends TemplateProvider<T>> Builder template(Class<P> providerClass) {
			this.templateRegistry.registerTemplate(providerClass);
			return this;
		}

		public Builder mergePolicy(String name, MergePolicy policy) {
			this.policyRegistry.register(name, policy);
			return this;
		}

		public Builder defaultMergePreset(MergePreset preset) {
			this.defaultMergePolicy = preset.policy();
			return this;
		}

		public Builder defaultMergePolicy(MergePolicy policy) {
			this.defaultMergePolicy = policy;
			return this;
		}

		public Config build() {
			return new Config(new Configura(extension, mapperFactory, modules, templateRegistry, policyRegistry, defaultMergePolicy));
		}
	}
}
