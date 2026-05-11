package me.whereareiam.configura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.builder.PolymorphicBuilder;
import me.whereareiam.configura.common.Configura;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.merge.defaults.DefaultMergeDefaultsRegistry;
import me.whereareiam.configura.common.migration.MigrationDefinitionRegistry;
import me.whereareiam.configura.common.polymorphic.PolymorphicRegistry;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.merge.MergeDefaultsProvider;
import me.whereareiam.configura.merge.MergeStrategy;
import me.whereareiam.configura.merge.MergeStrategyRegistry;
import me.whereareiam.configura.merge.strategy.DeepDefaults;
import me.whereareiam.configura.migration.MigrationDefinition;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
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
	private static final DefaultMergeDefaultsRegistry BOOTSTRAP_DEFAULTS_REGISTRY = new DefaultMergeDefaultsRegistry();
	private static Config defaultConfig = builder().format(Format.YAML).build();

	private final String extension;
	private final Function<List<Module>, ObjectMapper> mapperFactory;
	private final List<Module> modules;
	private final DefaultMergeDefaultsRegistry defaultsRegistry;
	private final MergeStrategyRegistry strategyRegistry;
	private final MigrationDefinitionRegistry versionedRegistry;
	private final Class<? extends MergeStrategy> defaultMergeStrategy;
	private final boolean backupOnMigration;
	private final Configura runtime;

	private Config(
			String extension,
			Function<List<Module>, ObjectMapper> mapperFactory,
			List<Module> modules,
			DefaultMergeDefaultsRegistry defaultsRegistry,
			MergeStrategyRegistry strategyRegistry,
			MigrationDefinitionRegistry versionedRegistry,
			Class<? extends MergeStrategy> defaultMergeStrategy,
			boolean backupOnMigration
	) {
		this.extension = extension;
		this.mapperFactory = mapperFactory;
		this.modules = List.copyOf(modules);
		this.defaultsRegistry = defaultsRegistry.copy();
		this.strategyRegistry = strategyRegistry.copy();
		this.versionedRegistry = versionedRegistry.copy();
		this.defaultMergeStrategy = defaultMergeStrategy;
		this.backupOnMigration = backupOnMigration;
		this.runtime = new Configura(
				extension,
				mapperFactory,
				this.modules,
				this.defaultsRegistry,
				this.strategyRegistry,
				this.versionedRegistry,
				this.defaultMergeStrategy,
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

	public static Config yaml() {
		return builder().format(Format.YAML).build();
	}

	public static Config json() {
		return builder().format(Format.JSON).build();
	}

	public static ConfigReader reader() {
		return new DefaultConfigReader().withFormat(defaultFormat());
	}

	public static ConfigReader reader(Format format) {
		return new DefaultConfigReader().withFormat(format);
	}

	public static ConfigWriter writer() {
		return new DefaultConfigWriter().withFormat(defaultFormat());
	}

	public static ConfigWriter writer(Format format) {
		return new DefaultConfigWriter().withFormat(format);
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
		return new Config(extension, mapperFactory, next, defaultsRegistry, strategyRegistry, versionedRegistry, defaultMergeStrategy, backupOnMigration);
	}

	public <T, P extends MergeDefaultsProvider<T>> Config withDefaults(Class<P> providerClass) {
		DefaultMergeDefaultsRegistry registry = defaultsRegistry.copy();
		registry.registerDefaults(providerClass);
		return new Config(extension, mapperFactory, modules, registry, strategyRegistry, versionedRegistry, defaultMergeStrategy, backupOnMigration);
	}

	public Config withMergeStrategy(String name, Class<? extends MergeStrategy> strategy) {
		MergeStrategyRegistry next = strategyRegistry.copy();
		next.register(name, strategy);
		return new Config(extension, mapperFactory, modules, defaultsRegistry, next, versionedRegistry, defaultMergeStrategy, backupOnMigration);
	}

	public Config withDefaultMergeStrategy(Class<? extends MergeStrategy> strategy) {
		return new Config(extension, mapperFactory, modules, defaultsRegistry, strategyRegistry, versionedRegistry, strategy, backupOnMigration);
	}

	public Config withBackupOnMigration(boolean backupOnMigration) {
		return new Config(extension, mapperFactory, modules, defaultsRegistry, strategyRegistry, versionedRegistry, defaultMergeStrategy, backupOnMigration);
	}

	public <T> Config withVersioned(Class<T> type, Consumer<MigrationDefinition<T>> customizer) {
		MigrationDefinition<T> definition = new MigrationDefinition<>(type);
		if (customizer != null)
			customizer.accept(definition);
		MigrationDefinitionRegistry next = versionedRegistry.copy();
		next.register(definition);
		return new Config(extension, mapperFactory, modules, defaultsRegistry, strategyRegistry, next, defaultMergeStrategy, backupOnMigration);
	}

	public String extension() {
		return extension;
	}

	public List<Module> modules() {
		return modules;
	}

	public DefaultMergeDefaultsRegistry registeredDefaults() {
		return defaultsRegistry.copy();
	}

	public Map<String, Class<? extends MergeStrategy>> mergeStrategies() {
		return strategyRegistry.asMap();
	}

	public boolean isVersioned(Class<?> type) {
		return versionedRegistry.contains(type);
	}

	public Class<? extends MergeStrategy> defaultMergeStrategy() {
		return defaultMergeStrategy;
	}

	public boolean backupOnMigration() {
		return backupOnMigration;
	}

	public static final class Builder {
		private String extension;
		private Function<List<Module>, ObjectMapper> mapperFactory;
		private final List<Module> modules = new ArrayList<>();
		private final DefaultMergeDefaultsRegistry defaultsRegistry;
		private final MergeStrategyRegistry strategyRegistry;
		private final MigrationDefinitionRegistry versionedRegistry;
		private Class<? extends MergeStrategy> defaultMergeStrategy;
		private boolean backupOnMigration;

		private Builder() {
			Config defaults = defaultConfig;
			if (defaults == null) {
				this.extension = Format.YAML.getExtension();
				this.mapperFactory = MapperFactory::createYamlMapper;
				this.defaultsRegistry = BOOTSTRAP_DEFAULTS_REGISTRY.copy();
				this.strategyRegistry = MergeStrategyRegistry.standard();
				this.versionedRegistry = new MigrationDefinitionRegistry();
				this.defaultMergeStrategy = DeepDefaults.class;
				this.backupOnMigration = true;
				return;
			}

			this.extension = defaults.extension;
			this.mapperFactory = defaults.mapperFactory;
			this.modules.addAll(defaults.modules);
			this.defaultsRegistry = defaults.defaultsRegistry.copy();
			this.strategyRegistry = defaults.strategyRegistry.copy();
			this.versionedRegistry = defaults.versionedRegistry.copy();
			this.defaultMergeStrategy = defaults.defaultMergeStrategy;
			this.backupOnMigration = defaults.backupOnMigration;
		}

		private Builder(Config source) {
			this.extension = source.extension;
			this.mapperFactory = source.mapperFactory;
			this.modules.addAll(source.modules);
			this.defaultsRegistry = source.defaultsRegistry.copy();
			this.strategyRegistry = source.strategyRegistry.copy();
			this.versionedRegistry = source.versionedRegistry.copy();
			this.defaultMergeStrategy = source.defaultMergeStrategy;
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

		public <T, P extends MergeDefaultsProvider<T>> Builder defaults(Class<P> providerClass) {
			this.defaultsRegistry.registerDefaults(providerClass);
			return this;
		}

		public Builder mergeStrategy(String name, Class<? extends MergeStrategy> strategy) {
			this.strategyRegistry.register(name, strategy);
			return this;
		}

		public Builder defaultMergeStrategy(Class<? extends MergeStrategy> strategy) {
			this.defaultMergeStrategy = strategy;
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
			return new Config(extension, mapperFactory, modules, defaultsRegistry, strategyRegistry, versionedRegistry, defaultMergeStrategy, backupOnMigration);
		}
	}
}
