package me.whereareiam.configura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.merge.defaults.DefaultsProviderRegistry;
import me.whereareiam.configura.common.merge.type.list.ListTypeAdapter;
import me.whereareiam.configura.common.merge.type.map.MapTypeAdapter;
import me.whereareiam.configura.common.merge.type.object.ObjectTypeAdapter;
import me.whereareiam.configura.common.merge.type.value.ValueTypeAdapter;
import me.whereareiam.configura.common.migration.MigrationDefinitionRegistry;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.merge.defaults.DefaultsResolver;
import me.whereareiam.configura.merge.defaults.DefaultsResolverRegistry;
import me.whereareiam.configura.merge.policy.AnnotationMergePolicyResolver;
import me.whereareiam.configura.merge.policy.MergePolicyResolver;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.strategy.*;
import me.whereareiam.configura.merge.type.BuiltinStrategyCapabilities;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.MergeTypeAdapterRegistry;
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
	private static final DefaultsProviderRegistry BOOTSTRAP_DEFAULT_PROVIDER_REGISTRY = new DefaultsProviderRegistry();

	private static Configura bootstrapConfig = builder().format(Format.YAML).build();
	private static Configura configuredConfig = bootstrapConfig;

	public static Builder builder() {
		return new Builder();
	}

	public static Configura configured() {
		return bootstrapConfig;
	}

	public static void setConfigured(Configura config) {
		Configura configured = Objects.requireNonNull(config, "config");
		bootstrapConfig = configured;
		configuredConfig = configured;
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
		private final List<ConfiguraFeature> features = new ArrayList<>();

		private final DefaultsProviderRegistry defaultProviderRegistry;
		private final MergeStrategyRegistry strategyRegistry;
		private final MergeTypeAdapterRegistry adapterRegistry;
		private final DefaultsResolverRegistry defaultsResolverRegistry;
		private final MergePolicyResolverRegistry policyResolverRegistry;
		private final MigrationDefinitionRegistry versionedRegistry;

		private Class<? extends FieldMergeStrategy> defaultStrategy;
		private String defaultStrategyName;
		private MergeBehavior mergeBehavior;
		private boolean backupOnMigration;

		private Builder() {
			Configura configured = configuredConfig;
			if (configured == null) {
				this.extension = Format.YAML.getExtension();

				this.mapperFactory = MapperFactory::createYamlMapper;

				this.defaultProviderRegistry = BOOTSTRAP_DEFAULT_PROVIDER_REGISTRY.copy();
				this.strategyRegistry = new MergeStrategyRegistry()
						.register(MergeStrategyDefinition.builder(DeepDefaults.class)
								.alias("deepDefaults")
								.capability(BuiltinStrategyCapabilities.LIST, new BuiltinStrategyCapabilities.ListCapability(BuiltinStrategyCapabilities.ListCapability.Mode.DEEP_DEFAULTS))
								.capability(BuiltinStrategyCapabilities.MAP, new BuiltinStrategyCapabilities.MapCapability(BuiltinStrategyCapabilities.MapCapability.Mode.DEEP_DEFAULTS))
								.build())
						.register(MergeStrategyDefinition.builder(SourceOwnsField.class)
								.alias("sourceOwnsField")
								.capability(BuiltinStrategyCapabilities.LIST, new BuiltinStrategyCapabilities.ListCapability(BuiltinStrategyCapabilities.ListCapability.Mode.SOURCE_OWNS))
								.capability(BuiltinStrategyCapabilities.MAP, new BuiltinStrategyCapabilities.MapCapability(BuiltinStrategyCapabilities.MapCapability.Mode.SOURCE_OWNS))
								.build())
						.register(MergeStrategyDefinition.builder(NeverDefaults.class)
								.alias("neverDefaults")
								.capability(BuiltinStrategyCapabilities.LIST, new BuiltinStrategyCapabilities.ListCapability(BuiltinStrategyCapabilities.ListCapability.Mode.NEVER_DEFAULTS))
								.capability(BuiltinStrategyCapabilities.MAP, new BuiltinStrategyCapabilities.MapCapability(BuiltinStrategyCapabilities.MapCapability.Mode.NEVER_DEFAULTS))
								.build())
						.register(MergeStrategyDefinition.builder(StructuralObject.class)
								.alias("structuralObject")
								.build())
						.register(MergeStrategyDefinition.builder(DeclaredObjectDefaults.class)
								.alias("declaredObjectDefaults")
								.build());
				this.adapterRegistry = new MergeTypeAdapterRegistry()
						.register(new ValueTypeAdapter())
						.register(new ObjectTypeAdapter())
						.register(new MapTypeAdapter())
						.register(new ListTypeAdapter());
				this.defaultsResolverRegistry = new DefaultsResolverRegistry()
						.register(new me.whereareiam.configura.merge.defaults.AnnotationMergeDefaultsResolver());
				this.policyResolverRegistry = new MergePolicyResolverRegistry()
						.register(new AnnotationMergePolicyResolver());
				this.versionedRegistry = new MigrationDefinitionRegistry();

				this.defaultStrategy = DeepDefaults.class;
				this.defaultStrategyName = null;
				this.mergeBehavior = MergeBehavior.defaults();
				this.backupOnMigration = true;
				return;
			}

			this.extension = configured.extension();

			this.mapperFactory = configured.mapperFactory();
			this.modules.addAll(configured.modules());
			this.features.addAll(configured.features());

			this.defaultProviderRegistry = configured.registeredDefaultProviders();
			this.strategyRegistry = configured.strategyRegistry();
			this.adapterRegistry = configured.typeAdapterRegistry();
			this.defaultsResolverRegistry = configured.defaultsResolverRegistry();
			this.policyResolverRegistry = configured.policyResolverRegistry();
			this.versionedRegistry = configured.versionedRegistry();

			this.defaultStrategy = configured.defaultStrategy();
			this.defaultStrategyName = configured.defaultStrategyName();
			this.mergeBehavior = configured.mergeBehavior();
			this.backupOnMigration = configured.backupOnMigration();
		}

		private Builder(Configura source) {
			this.extension = source.extension();

			this.mapperFactory = source.mapperFactory();
			this.modules.addAll(source.modules());
			this.features.addAll(source.features());

			this.defaultProviderRegistry = source.registeredDefaultProviders();
			this.strategyRegistry = source.strategyRegistry();
			this.adapterRegistry = source.typeAdapterRegistry();
			this.defaultsResolverRegistry = source.defaultsResolverRegistry();
			this.policyResolverRegistry = source.policyResolverRegistry();
			this.versionedRegistry = source.versionedRegistry();

			this.defaultStrategy = source.defaultStrategy();
			this.defaultStrategyName = source.defaultStrategyName();
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

		public <T, P extends DefaultsProvider<T>> Builder defaults(Class<P> providerClass) {
			this.defaultProviderRegistry.registerProvider(providerClass);
			return this;
		}

		public Builder feature(ConfiguraFeature feature) {
			if (feature != null) this.features.add(feature);
			return this;
		}

		public Builder mergeStrategy(String name, Class<? extends FieldMergeStrategy> strategy) {
			this.strategyRegistry.registerAlias(name, strategy);
			return this;
		}

		public Builder mergeTypeAdapter(MergeTypeAdapter adapter) {
			this.adapterRegistry.register(adapter);
			return this;
		}

		public Builder defaultsResolver(DefaultsResolver resolver) {
			this.defaultsResolverRegistry.register(resolver);
			return this;
		}

		public Builder mergeStrategy(MergeStrategyDefinition definition) {
			this.strategyRegistry.register(definition);
			return this;
		}

		public Builder mergeStrategy(
				Class<? extends FieldMergeStrategy> strategy,
				Consumer<MergeStrategyDefinition.Builder> customizer
		) {
			MergeStrategyDefinition.Builder builder = MergeStrategyDefinition.builder(strategy);
			if (customizer != null) customizer.accept(builder);
			this.strategyRegistry.register(builder.build());
			return this;
		}

		public Builder policyResolver(MergePolicyResolver resolver) {
			this.policyResolverRegistry.register(resolver);
			return this;
		}

		public Builder defaultStrategy(Class<? extends FieldMergeStrategy> strategy) {
			this.defaultStrategy = strategy;
			this.defaultStrategyName = null;
			return this;
		}

		public Builder defaultStrategy(String strategyName) {
			this.defaultStrategyName = strategyName;
			this.defaultStrategy = null;
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
					features,
					defaultProviderRegistry,
					strategyRegistry,
					adapterRegistry,
					defaultsResolverRegistry,
					policyResolverRegistry,
					versionedRegistry,
					defaultStrategy,
					defaultStrategyName,
					mergeBehavior,
					backupOnMigration
			);
		}
	}
}
