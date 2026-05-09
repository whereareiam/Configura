package me.whereareiam.configura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.builder.PolymorphicBuilder;
import me.whereareiam.configura.common.Configura;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.migration.MigrationDefinitionRegistry;
import me.whereareiam.configura.common.polymorphic.PolymorphicRegistry;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.template.DefaultTemplateRegistry;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.merge.MergePolicy;
import me.whereareiam.configura.merge.MergePolicyRegistry;
import me.whereareiam.configura.migration.MigrationDefinition;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.type.MergePreset;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Config {
	private static final DefaultTemplateRegistry BOOTSTRAP_TEMPLATE_REGISTRY = new DefaultTemplateRegistry();
	private static Config defaultConfig = builder().format(Format.YAML).build();

	private final String extension;
	private final Function<List<Module>, ObjectMapper> mapperFactory;
	private final List<Module> modules;
	private final DefaultTemplateRegistry templateRegistry;
	private final MergePolicyRegistry policyRegistry;
	private final MigrationDefinitionRegistry versionedRegistry;
	private final MergePolicy defaultMergePolicy;
	private final boolean backupOnMigration;
	private final Configura runtime;

	private Config(
			String extension,
			Function<List<Module>, ObjectMapper> mapperFactory,
			List<Module> modules,
			DefaultTemplateRegistry templateRegistry,
			MergePolicyRegistry policyRegistry,
			MigrationDefinitionRegistry versionedRegistry,
			MergePolicy defaultMergePolicy,
			boolean backupOnMigration
	) {
		this.extension = extension;
		this.mapperFactory = mapperFactory;
		this.modules = List.copyOf(modules);
		this.templateRegistry = templateRegistry.copy();
		this.policyRegistry = policyRegistry.copy();
		this.versionedRegistry = versionedRegistry.copy();
		this.defaultMergePolicy = defaultMergePolicy;
		this.backupOnMigration = backupOnMigration;
		this.runtime = new Configura(
				extension,
				mapperFactory,
				this.modules,
				this.templateRegistry,
				this.policyRegistry,
				this.versionedRegistry,
				this.defaultMergePolicy,
				this.backupOnMigration
		);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Config defaults() {
		return defaultConfig;
	}

	public static void setDefaults(Config config) {
		defaultConfig = Objects.requireNonNull(config, "config");
	}

	public static void reconfigureDefaults(Consumer<Builder> customizer) {
		Builder builder = new Builder(defaults());
		if (customizer != null)
			customizer.accept(builder);
		setDefaults(builder.build());
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
		return new DefaultConfigReader()
				.withTemplateRegistry(defaults().registeredTemplates())
				.withFormat(defaultFormat());
	}

	/**
	 * Create a new independent reader instance with the given format.
	 */
	public static ConfigReader reader(Format format) {
		return new DefaultConfigReader()
				.withTemplateRegistry(defaults().registeredTemplates())
				.withFormat(format);
	}

	/**
	 * Create a new independent writer instance with default settings.
	 */
	public static ConfigWriter writer() {
		return new DefaultConfigWriter()
				.withTemplateRegistry(defaults().registeredTemplates())
				.withFormat(defaultFormat());
	}

	/**
	 * Create a new independent writer instance with the given format.
	 */
	public static ConfigWriter writer(Format format) {
		return new DefaultConfigWriter()
				.withTemplateRegistry(defaults().registeredTemplates())
				.withFormat(format);
	}

	/**
	 * Create a new template registry instance with default resolver.
	 */
	public static TemplateRegistry templateRegistry() {
		return new DefaultTemplateRegistry();
	}

	public static <T> PolymorphicBuilder<T> registerPolymorphic(Class<T> baseType) {
		return PolymorphicRegistry.register(baseType);
	}

	private static Format inferFormat(String extension) {
		if (Format.JSON.getExtension().equals(extension))
			return Format.JSON;
		if (Format.YAML.getExtension().equals(extension))
			return Format.YAML;
		return null;
	}

	private static Format defaultFormat() {
		Format format = inferFormat(defaults().extension());
		return format != null ? format : Format.YAML;
	}

	public ObjectMapper mapper() {
		return runtime.mapper();
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

	public JsonNode readNode(String file) {
		return runtime.readTree(file);
	}

	public JsonNode readNode(Path path) {
		return runtime.readTree(path);
	}

	public JsonNode readNode(byte[] bytes) {
		return runtime.readTree(bytes);
	}

	public JsonNode readNode(InputStream inputStream) {
		return runtime.readTree(inputStream);
	}

	public <T> JsonNode readMigratedNode(String file, Class<T> type) {
		return runtime.readMigratedTree(file, type);
	}

	public <T> JsonNode readMigratedNode(Path path, Class<T> type) {
		return runtime.readMigratedTree(path, type);
	}

	public <T> JsonNode readMigratedNode(byte[] bytes, Class<T> type) {
		return runtime.readMigratedTree(bytes, type);
	}

	public <T> JsonNode readMigratedNode(InputStream inputStream, Class<T> type) {
		return runtime.readMigratedTree(inputStream, type);
	}

	public void writeNode(String file, JsonNode node) {
		runtime.writeTree(file, node);
	}

	public void writeNode(Path path, JsonNode node) {
		runtime.writeTree(path, node);
	}

	public byte[] writeNodeBytes(JsonNode node) {
		return runtime.writeTreeBytes(node);
	}

	public Config withModule(Module module) {
		List<Module> next = new ArrayList<>(modules);
		if (module != null) next.add(module);
		return new Config(extension, mapperFactory, next, templateRegistry, policyRegistry, versionedRegistry, defaultMergePolicy, backupOnMigration);
	}

	public <T, P extends TemplateProvider<T>> Config withTemplate(Class<P> providerClass) {
		DefaultTemplateRegistry registry = templateRegistry.copy();
		registry.registerTemplate(providerClass);
		return new Config(extension, mapperFactory, modules, registry, policyRegistry, versionedRegistry, defaultMergePolicy, backupOnMigration);
	}

	public Config withMergePolicy(String name, MergePolicy policy) {
		MergePolicyRegistry next = policyRegistry.copy();
		next.register(name, policy);
		return new Config(extension, mapperFactory, modules, templateRegistry, next, versionedRegistry, defaultMergePolicy, backupOnMigration);
	}

	public Config withDefaultMergePreset(MergePreset preset) {
		return new Config(extension, mapperFactory, modules, templateRegistry, policyRegistry, versionedRegistry, preset.policy(), backupOnMigration);
	}

	public Config withDefaultMergePolicy(MergePolicy policy) {
		return new Config(extension, mapperFactory, modules, templateRegistry, policyRegistry, versionedRegistry, policy, backupOnMigration);
	}

	public Config withBackupOnMigration(boolean backupOnMigration) {
		return new Config(extension, mapperFactory, modules, templateRegistry, policyRegistry, versionedRegistry, defaultMergePolicy, backupOnMigration);
	}

	public <T> Config withVersioned(Class<T> type, Consumer<MigrationDefinition<T>> customizer) {
		MigrationDefinition<T> definition = new MigrationDefinition<>(type);
		if (customizer != null)
			customizer.accept(definition);
		MigrationDefinitionRegistry next = versionedRegistry.copy();
		next.register(definition);
		return new Config(extension, mapperFactory, modules, templateRegistry, policyRegistry, next, defaultMergePolicy, backupOnMigration);
	}

	public String extension() {
		return extension;
	}

	public List<Module> modules() {
		return modules;
	}

	public DefaultTemplateRegistry registeredTemplates() {
		return templateRegistry.copy();
	}

	public Map<String, MergePolicy> mergePolicies() {
		return policyRegistry.asMap();
	}

	public boolean isVersioned(Class<?> type) {
		return versionedRegistry.contains(type);
	}

	public MergePolicy defaultMergePolicy() {
		return defaultMergePolicy;
	}

	public boolean backupOnMigration() {
		return backupOnMigration;
	}

	public static final class Builder {
		private String extension;
		private Function<List<Module>, ObjectMapper> mapperFactory;
		private final List<Module> modules = new ArrayList<>();
		private final DefaultTemplateRegistry templateRegistry;
		private final MergePolicyRegistry policyRegistry;
		private final MigrationDefinitionRegistry versionedRegistry;
		private MergePolicy defaultMergePolicy;
		private boolean backupOnMigration;

		private Builder() {
			Config defaults = defaultConfig;
			if (defaults == null) {
				this.extension = Format.YAML.getExtension();
				this.mapperFactory = MapperFactory::createYamlMapper;
				this.templateRegistry = BOOTSTRAP_TEMPLATE_REGISTRY.copy();
				this.policyRegistry = MergePolicyRegistry.standard();
				this.versionedRegistry = new MigrationDefinitionRegistry();
				this.defaultMergePolicy = MergePreset.DEEP_DEFAULTS.policy();
				this.backupOnMigration = true;
				return;
			}

			this.extension = defaults.extension;
			this.mapperFactory = defaults.mapperFactory;
			this.modules.addAll(defaults.modules);
			this.templateRegistry = defaults.templateRegistry.copy();
			this.policyRegistry = defaults.policyRegistry.copy();
			this.versionedRegistry = defaults.versionedRegistry.copy();
			this.defaultMergePolicy = defaults.defaultMergePolicy;
			this.backupOnMigration = defaults.backupOnMigration;
		}

		private Builder(Config source) {
			this.extension = source.extension;
			this.mapperFactory = source.mapperFactory;
			this.modules.addAll(source.modules);
			this.templateRegistry = source.templateRegistry.copy();
			this.policyRegistry = source.policyRegistry.copy();
			this.versionedRegistry = source.versionedRegistry.copy();
			this.defaultMergePolicy = source.defaultMergePolicy;
			this.backupOnMigration = source.backupOnMigration;
		}

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
			if (module != null) this.modules.add(module);
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

		public Builder backupOnMigration(boolean backupOnMigration) {
			this.backupOnMigration = backupOnMigration;
			return this;
		}

		public <T> Builder versioned(Class<T> type, Consumer<MigrationDefinition<T>> customizer) {
			MigrationDefinition<T> definition = new MigrationDefinition<>(type);
			if (customizer != null) customizer.accept(definition);
			this.versionedRegistry.register(definition);
			return this;
		}

		public Config build() {
			return new Config(extension, mapperFactory, modules, templateRegistry, policyRegistry, versionedRegistry, defaultMergePolicy, backupOnMigration);
		}
	}
}
