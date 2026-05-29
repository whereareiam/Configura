package me.whereareiam.configura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.builder.PolymorphicBuilder;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.merge.defaults.MergeDefaultsProviderRegistry;
import me.whereareiam.configura.common.migration.MigrationDefinitionRegistry;
import me.whereareiam.configura.common.polymorphic.PolymorphicRegistry;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.configura.merge.plugin.MergePlugin;
import me.whereareiam.configura.merge.plugin.MergePluginRegistry;
import me.whereareiam.configura.merge.plugin.list.ListMergePlugin;
import me.whereareiam.configura.merge.plugin.map.MapMergePlugin;
import me.whereareiam.configura.merge.plugin.property.PropertyMergePlugin;
import me.whereareiam.configura.merge.policy.AnnotationMergePolicyResolver;
import me.whereareiam.configura.merge.policy.MergePolicyResolver;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.strategy.*;
import me.whereareiam.configura.migration.MigrationDefinition;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Config {
	private static final MergeDefaultsProviderRegistry BOOTSTRAP_DEFAULT_PROVIDER_REGISTRY = new MergeDefaultsProviderRegistry();

	private static Configura bootstrapConfig = builder().format(Format.YAML).build();
	private static Configura configuredConfig = bootstrapConfig;

	public static Builder builder() {
		return new Builder();
	}

	public static Configura defaults() {
		return bootstrapConfig;
	}

	public static void setDefaults(Configura config) {
		Configura defaults = Objects.requireNonNull(config, "config");
		bootstrapConfig = defaults;
		configuredConfig = defaults;
	}

	public static void configure(Consumer<Builder> customizer) {
		Builder builder = new Builder(configuredConfig);
		if (customizer != null) customizer.accept(builder);

		configuredConfig = builder.build();
	}

	public static Configura yaml() {
		return builder().format(Format.YAML).build();
	}

	public static Configura json() {
		return builder().format(Format.JSON).build();
	}

	public static ConfigReader reader() {
		return reader(configuredConfig);
	}

	public static ConfigReader reader(Format format) {
		return reader(builder().format(format).build());
	}

	public static ConfigReader reader(Configura configura) {
		return Objects.requireNonNull(configura, "configura").reader();
	}

	public static ConfigWriter writer() {
		return writer(configuredConfig);
	}

	public static ConfigWriter writer(Format format) {
		return writer(builder().format(format).build());
	}

	public static ConfigWriter writer(Configura configura) {
		return Objects.requireNonNull(configura, "configura").writer();
	}

	public static <T> PolymorphicBuilder<T> registerPolymorphic(Class<T> baseType) {
		return PolymorphicRegistry.register(baseType);
	}

	public static ObjectMapper mapper() {
		return configuredConfig.mapper();
	}

	public static <T> T read(String file, Class<T> type) {
		return configuredConfig.read(file, type);
	}

	public static <T> T read(Path path, Class<T> type) {
		return configuredConfig.read(path, type);
	}

	public static <T> T read(byte[] bytes, Class<T> type) {
		return configuredConfig.read(bytes, type);
	}

	public static <T> T read(InputStream inputStream, Class<T> type) {
		return configuredConfig.read(inputStream, type);
	}

	public static <T> void write(String file, T value) {
		configuredConfig.write(file, value);
	}

	public static <T> void write(Path path, T value) {
		configuredConfig.write(path, value);
	}

	public static <T> void save(String file, T value) {
		configuredConfig.save(file, value);
	}

	public static <T> void save(Path path, T value) {
		configuredConfig.save(path, value);
	}

	public static <T> byte[] writeBytes(T value) {
		return configuredConfig.writeBytes(value);
	}

	public static <T> T merge(String file, T value) {
		return configuredConfig.merge(file, value);
	}

	public static <T> T merge(Path path, T value) {
		return configuredConfig.merge(path, value);
	}

	public static <T> T update(String file, Class<T> type) {
		return configuredConfig.update(file, type);
	}

	public static <T> T update(Path path, Class<T> type) {
		return configuredConfig.update(path, type);
	}

	public static JsonNode readNode(String file) {
		return configuredConfig.readNode(file);
	}

	public static JsonNode readNode(Path path) {
		return configuredConfig.readNode(path);
	}

	public static JsonNode readNode(byte[] bytes) {
		return configuredConfig.readNode(bytes);
	}

	public static JsonNode readNode(InputStream inputStream) {
		return configuredConfig.readNode(inputStream);
	}

	public static <T> JsonNode readResolvedNode(String file, Class<T> type) {
		return configuredConfig.readResolvedNode(file, type);
	}

	public static <T> JsonNode readResolvedNode(Path path, Class<T> type) {
		return configuredConfig.readResolvedNode(path, type);
	}

	public static <T> JsonNode readResolvedNode(byte[] bytes, Class<T> type) {
		return configuredConfig.readResolvedNode(bytes, type);
	}

	public static <T> JsonNode readResolvedNode(InputStream inputStream, Class<T> type) {
		return configuredConfig.readResolvedNode(inputStream, type);
	}

	public static void writeNode(String file, JsonNode node) {
		configuredConfig.writeNode(file, node);
	}

	public static void writeNode(Path path, JsonNode node) {
		configuredConfig.writeNode(path, node);
	}

	public static byte[] writeNodeBytes(JsonNode node) {
		return configuredConfig.writeNodeBytes(node);
	}

	public static final class Builder {
		private String extension;

		private Function<List<Module>, ObjectMapper> mapperFactory;
		private final List<Module> modules = new ArrayList<>();

		private final MergeDefaultsProviderRegistry defaultProviderRegistry;
		private final FieldMergeStrategyRegistry strategyRegistry;
		private final MergePluginRegistry pluginRegistry;
		private final MergePolicyResolverRegistry policyResolverRegistry;
		private final MigrationDefinitionRegistry versionedRegistry;

		private Class<? extends FieldMergeStrategy> defaultStrategy;
		private MergeBehavior mergeBehavior;
		private boolean backupOnMigration;

		private Builder() {
			Configura configured = configuredConfig;
			if (configured == null) {
				this.extension = Format.YAML.getExtension();

				this.mapperFactory = MapperFactory::createYamlMapper;

				this.defaultProviderRegistry = BOOTSTRAP_DEFAULT_PROVIDER_REGISTRY.copy();
				this.strategyRegistry = FieldMergeStrategyRegistry.standard();
				this.strategyRegistry
						.register("deepDefaults", DeepDefaults.class)
						.register("sourceOwnsField", SourceOwnsField.class)
						.register("neverDefaults", NeverDefaults.class)
						.register("structuralObject", StructuralObject.class);
				this.pluginRegistry = new MergePluginRegistry()
						.register(new PropertyMergePlugin())
						.register(new MapMergePlugin())
						.register(new ListMergePlugin());
				this.policyResolverRegistry = new MergePolicyResolverRegistry()
						.register(new AnnotationMergePolicyResolver());
				this.versionedRegistry = new MigrationDefinitionRegistry();

				this.defaultStrategy = DeepDefaults.class;
				this.mergeBehavior = MergeBehavior.defaults();
				this.backupOnMigration = true;
				return;
			}

			this.extension = configured.extension();

			this.mapperFactory = configured.mapperFactory();
			this.modules.addAll(configured.modules());

			this.defaultProviderRegistry = configured.registeredDefaultProviders();
			this.strategyRegistry = configured.strategyRegistry();
			this.pluginRegistry = configured.pluginRegistry();
			this.policyResolverRegistry = configured.policyResolverRegistry();
			this.versionedRegistry = configured.versionedRegistry();

			this.defaultStrategy = configured.defaultStrategy();
			this.mergeBehavior = configured.mergeBehavior();
			this.backupOnMigration = configured.backupOnMigration();
		}

		private Builder(Configura source) {
			this.extension = source.extension();

			this.mapperFactory = source.mapperFactory();
			this.modules.addAll(source.modules());

			this.defaultProviderRegistry = source.registeredDefaultProviders();
			this.strategyRegistry = source.strategyRegistry();
			this.pluginRegistry = source.pluginRegistry();
			this.policyResolverRegistry = source.policyResolverRegistry();
			this.versionedRegistry = source.versionedRegistry();

			this.defaultStrategy = source.defaultStrategy();
			this.mergeBehavior = source.mergeBehavior();
			this.backupOnMigration = source.backupOnMigration();
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
			this.defaultProviderRegistry.registerProvider(providerClass);
			return this;
		}

		public Builder mergeStrategy(String name, Class<? extends FieldMergeStrategy> strategy) {
			this.strategyRegistry.register(name, strategy);
			return this;
		}

		public Builder mergePlugin(MergePlugin plugin) {
			this.pluginRegistry.register(plugin);
			return this;
		}

		public Builder policyResolver(MergePolicyResolver resolver) {
			this.policyResolverRegistry.register(resolver);
			return this;
		}

		public Builder defaultStrategy(Class<? extends FieldMergeStrategy> strategy) {
			this.defaultStrategy = strategy;
			return this;
		}

		public Builder mergeBehavior(MergeBehavior mergeBehavior) {
			this.mergeBehavior = mergeBehavior;
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

		public Configura build() {
			return new Configura(
					extension,
					mapperFactory,
					modules,
					defaultProviderRegistry,
					strategyRegistry,
					pluginRegistry,
					policyResolverRegistry,
					versionedRegistry,
					defaultStrategy,
					mergeBehavior,
					backupOnMigration
			);
		}
	}
}
