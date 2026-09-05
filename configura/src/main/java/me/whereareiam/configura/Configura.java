package me.whereareiam.configura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.ConfiguraFeatureRegistry;
import me.whereareiam.configura.common.document.DefaultDocumentProcessor;
import me.whereareiam.configura.common.merge.MergeEngine;
import me.whereareiam.configura.common.merge.defaults.DefaultsProviderRegistry;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.document.DocumentProcessor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.merge.defaults.DefaultsResolver;
import me.whereareiam.configura.merge.defaults.DefaultsResolverRegistry;
import me.whereareiam.configura.merge.policy.MergePolicyResolver;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.MergeStrategyDefinition;
import me.whereareiam.configura.merge.strategy.MergeStrategyRegistry;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.MergeTypeAdapterRegistry;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.writer.ConfigWriter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Configura {
	private final String extension;

	private final Function<List<Module>, ObjectMapper> mapperFactory;
	private final List<Module> modules;
	private final List<ConfiguraFeature> features;

	private final DefaultsProviderRegistry defaultProviderRegistry;
	private final MergeStrategyRegistry strategyRegistry;
	private final MergeTypeAdapterRegistry adapterRegistry;
	private final DefaultsResolverRegistry defaultsResolverRegistry;
	private final MergePolicyResolverRegistry policyResolverRegistry;

	private final Class<? extends FieldMergeStrategy> defaultStrategy;
	private final String defaultStrategyName;
	private final MergeBehavior mergeBehavior;

	private final ObjectMapper mapper;
	private final DocumentProcessor documentProcessor;
	private final MergeEngine mergeEngine;
	private final ConfigReader reader;
	private final ConfigWriter writer;

	Configura(
			String extension,

			Function<List<Module>, ObjectMapper> mapperFactory,
			List<Module> modules,
			List<ConfiguraFeature> features,

			DefaultsProviderRegistry defaultProviderRegistry,
			MergeStrategyRegistry strategyRegistry,
			MergeTypeAdapterRegistry adapterRegistry,
			DefaultsResolverRegistry defaultsResolverRegistry,
			MergePolicyResolverRegistry policyResolverRegistry,

			Class<? extends FieldMergeStrategy> defaultStrategy,
			String defaultStrategyName,
			MergeBehavior mergeBehavior
	) {
		this.extension = extension;

		this.mapperFactory = mapperFactory;
		this.modules = List.copyOf(modules);
		this.features = List.copyOf(features);

		this.defaultProviderRegistry = defaultProviderRegistry.copy();
		this.strategyRegistry = strategyRegistry.copy();
		this.adapterRegistry = adapterRegistry.copy();
		this.defaultsResolverRegistry = defaultsResolverRegistry.copy();
		this.policyResolverRegistry = policyResolverRegistry.copy();

		this.defaultStrategy = defaultStrategy;
		this.defaultStrategyName = defaultStrategyName;
		this.mergeBehavior = mergeBehavior != null ? mergeBehavior : MergeBehavior.defaults();

		ConfiguraFeatureRegistry featureRegistry = new ConfiguraFeatureRegistry();
		for (ConfiguraFeature feature : this.features)
			featureRegistry.add(feature);
		ObjectMapper plainMapper = mapperFactory.apply(this.modules);
		List<Module> mapperModules = new ArrayList<>(this.modules);
		mapperModules.addAll(featureRegistry.modules(plainMapper));
		this.mapper = mapperFactory.apply(mapperModules);
		this.documentProcessor = new DefaultDocumentProcessor(featureRegistry.typeResolvers(), featureRegistry.phases());
		this.mergeEngine = new MergeEngine(
				this.mapper,
				this.defaultProviderRegistry,
				this.documentProcessor,
				this.strategyRegistry,
				this.adapterRegistry,
				this.defaultsResolverRegistry,
				this.policyResolverRegistry,
				this.defaultStrategy,
				this.defaultStrategyName,
				this.mergeBehavior
		);

		this.reader = new DefaultConfigReader(this.extension, this.mapper, this.documentProcessor);
		this.writer = new DefaultConfigWriter(this.extension, this.mapper);
	}

	public ObjectMapper mapper() {
		return mapper;
	}

	Function<List<Module>, ObjectMapper> mapperFactory() {
		return mapperFactory;
	}

	MergeStrategyRegistry strategyRegistry() {
		return strategyRegistry.copy();
	}

	MergeTypeAdapterRegistry typeAdapterRegistry() {
		return adapterRegistry.copy();
	}

	DefaultsResolverRegistry defaultsResolverRegistry() {
		return defaultsResolverRegistry.copy();
	}

	MergePolicyResolverRegistry policyResolverRegistry() {
		return policyResolverRegistry.copy();
	}


	ConfigReader reader() {
		return reader;
	}

	ConfigWriter writer() {
		return writer;
	}

	public <T> T read(String file, Class<T> type) {
		return read(resolve(file), type);
	}

	public <T> T read(Path path, Class<T> type) {
		if (path == null) throw new ConfigException("path must not be null");
		if (type == null) throw new ConfigException("configClass must not be null");

		JsonNode resolved = readTree(resolve(path));
		return bind(resolved, type, "Failed to bind config to " + type.getName());
	}

	public <T> T read(byte[] bytes, Class<T> type) {
		if (type == null) throw new ConfigException("configClass must not be null");

		JsonNode resolved = readTree(bytes);
		return bind(resolved, type, "Failed to read config bytes for " + type.getName());
	}

	public <T> T read(InputStream inputStream, Class<T> type) {
		if (type == null) throw new ConfigException("configClass must not be null");

		JsonNode resolved = readTree(inputStream);
		return bind(resolved, type, "Failed to read config stream for " + type.getName());
	}

	public <T> void write(String file, T value) {
		if (file == null) throw new NullPointerException("file");
		write(Path.of(file), value);
	}

	public <T> void write(Path path, T value) {
		if (value == null) {
			writer.write(path, null);
			return;
		}

		writer.writeNode(path, mapper.valueToTree(value));
	}

	public <T> void save(String file, T value) {
		if (file == null) throw new NullPointerException("file");
		save(Path.of(file), value);
	}

	@SuppressWarnings("unchecked")
	public <T> void save(Path path, T value) {
		if (value == null) throw new NullPointerException("value");

		Class<T> type = (Class<T>) value.getClass();
		JsonNode existing = existingResolvedTree(path, type);

		ObjectNode source = mapper.valueToTree(value);
		ObjectNode merged = mergeUserModel(source, value, type);
		writer.writeNode(path, merged);
	}

	public <T> byte[] writeBytes(T value) {
		if (value == null)
			return writer.writeBytes(null);

		return writer.writeBytes(mapper.valueToTree(value));
	}

	public <T> T merge(String file, T value) {
		return merge(resolve(file), value);
	}

	@SuppressWarnings("unchecked")
	public <T> T merge(Path path, T value) {
		if (value == null) throw new ConfigException("config must not be null");

		Class<T> type = (Class<T>) value.getClass();
		JsonNode existing = existingResolvedTree(path, type);
		ObjectNode merged = mergeDefaults(existing, value, type);
		return bind(merged, type, "Failed to bind merged config to " + type.getName());
	}

	public <T> T update(String file, Class<T> type) {
		return update(Path.of(file), type);
	}

	public <T> T update(Path path, Class<T> type) {
		T empty = instantiate(type);
		JsonNode existing = existingResolvedTree(path, type);

		ObjectNode merged = mergeDefaults(existing, empty, type);
		T value = bind(merged, type, "Failed to bind updated config to " + type.getName());
		writer.writeNode(path, merged);
		return value;
	}

	/** Prepares an in-memory document using configured defaults, merge policies and binding hooks.
	 * No file is read or written. This allows callers such as Strata to validate staged documents.
	 * @param source source document tree
	 * @param type target document model
	 * @param <T> target model type
	 * @return merged document that successfully binds to the target model
	 */
	public <T> @NotNull ObjectNode prepareNode(
			@NotNull JsonNode source,
			@NotNull Class<T> type
	) {
		ObjectNode merged = mergeDefaults(source, instantiate(type), type);
		bind(merged, type, "Failed to validate prepared config for " + type.getName());
		return merged;
	}

	public JsonNode readNode(String file) {
		if (file == null) throw new NullPointerException("file");
		return reader.readNode(Path.of(file));
	}

	public JsonNode readNode(Path path) {
		return reader.readNode(path);
	}

	public JsonNode readNode(byte[] bytes) {
		return reader.readNode(bytes);
	}

	public JsonNode readNode(InputStream inputStream) {
		return reader.readNode(inputStream);
	}

	public <T> JsonNode readResolvedNode(String file, Class<T> type) {
		return readResolvedNode(resolve(file), type);
	}

	public <T> JsonNode readResolvedNode(Path path, Class<T> type) {
		return readTree(resolve(path));
	}

	public <T> JsonNode readResolvedNode(byte[] bytes, Class<T> type) {
		return readTree(bytes);
	}

	public <T> JsonNode readResolvedNode(InputStream inputStream, Class<T> type) {
		return readTree(inputStream);
	}

	public void writeNode(String file, JsonNode node) {
		if (file == null) throw new NullPointerException("file");
		writer.writeNode(Path.of(file), node);
	}

	public void writeNode(Path path, JsonNode node) {
		writer.writeNode(path, node);
	}

	public byte[] writeNodeBytes(JsonNode node) {
		return writer.writeNodeBytes(node);
	}

	public Configura withModule(Module module) {
		List<Module> next = new ArrayList<>(modules);
		if (module != null) next.add(module);
		return new Configura(extension, mapperFactory, next, features, defaultProviderRegistry, strategyRegistry, adapterRegistry, defaultsResolverRegistry, policyResolverRegistry, defaultStrategy, defaultStrategyName, mergeBehavior);
	}

	public <T, P extends DefaultsProvider<T>> Configura withDefaults(Class<P> providerClass) {
		DefaultsProviderRegistry registry = defaultProviderRegistry.copy();
		registry.registerProvider(providerClass);
		return new Configura(extension, mapperFactory, modules, features, registry, strategyRegistry, adapterRegistry, defaultsResolverRegistry, policyResolverRegistry, defaultStrategy, defaultStrategyName, mergeBehavior);
	}

	public Configura withFeature(ConfiguraFeature feature) {
		List<ConfiguraFeature> next = new ArrayList<>(features);
		if (feature != null) next.add(feature);
		return new Configura(extension(), mapperFactory, modules, next, defaultProviderRegistry, strategyRegistry, adapterRegistry, defaultsResolverRegistry, policyResolverRegistry, defaultStrategy, defaultStrategyName, mergeBehavior);
	}

	public Configura withStrategy(String name, Class<? extends FieldMergeStrategy> strategy) {
		MergeStrategyRegistry next = strategyRegistry.copy();
		next.registerAlias(name, strategy);
		return new Configura(extension, mapperFactory, modules, features, defaultProviderRegistry, next, adapterRegistry, defaultsResolverRegistry, policyResolverRegistry, defaultStrategy, defaultStrategyName, mergeBehavior);
	}

	public Configura withStrategy(MergeStrategyDefinition definition) {
		MergeStrategyRegistry next = strategyRegistry.copy();
		next.register(definition);
		return new Configura(extension, mapperFactory, modules, features, defaultProviderRegistry, next, adapterRegistry, defaultsResolverRegistry, policyResolverRegistry, defaultStrategy, defaultStrategyName, mergeBehavior);
	}

	public Configura withTypeAdapter(MergeTypeAdapter adapter) {
		MergeTypeAdapterRegistry next = adapterRegistry.copy();
		next.register(adapter);
		return new Configura(extension, mapperFactory, modules, features, defaultProviderRegistry, strategyRegistry, next, defaultsResolverRegistry, policyResolverRegistry, defaultStrategy, defaultStrategyName, mergeBehavior);
	}

	public Configura withDefaultsResolver(DefaultsResolver resolver) {
		DefaultsResolverRegistry next = defaultsResolverRegistry.copy();
		next.register(resolver);
		return new Configura(extension, mapperFactory, modules, features, defaultProviderRegistry, strategyRegistry, adapterRegistry, next, policyResolverRegistry, defaultStrategy, defaultStrategyName, mergeBehavior);
	}

	public Configura withPolicyResolver(MergePolicyResolver resolver) {
		MergePolicyResolverRegistry next = policyResolverRegistry.copy();
		next.register(resolver);
		return new Configura(extension, mapperFactory, modules, features, defaultProviderRegistry, strategyRegistry, adapterRegistry, defaultsResolverRegistry, next, defaultStrategy, defaultStrategyName, mergeBehavior);
	}

	public Configura withDefaultStrategy(Class<? extends FieldMergeStrategy> strategy) {
		return new Configura(extension, mapperFactory, modules, features, defaultProviderRegistry, strategyRegistry, adapterRegistry, defaultsResolverRegistry, policyResolverRegistry, strategy, null, mergeBehavior);
	}

	public Configura withDefaultStrategy(String strategyName) {
		return new Configura(extension, mapperFactory, modules, features, defaultProviderRegistry, strategyRegistry, adapterRegistry, defaultsResolverRegistry, policyResolverRegistry, null, strategyName, mergeBehavior);
	}

	public Configura withMergeBehavior(MergeBehavior mergeBehavior) {
		return new Configura(extension, mapperFactory, modules, features, defaultProviderRegistry, strategyRegistry, adapterRegistry, defaultsResolverRegistry, policyResolverRegistry, defaultStrategy, defaultStrategyName, mergeBehavior);
	}



	public String extension() {
		return extension;
	}

	public List<Module> modules() {
		return modules;
	}

	public DefaultsProviderRegistry registeredDefaultProviders() {
		return defaultProviderRegistry.copy();
	}

	List<ConfiguraFeature> features() {
		return List.copyOf(features);
	}

	public Map<String, Class<? extends FieldMergeStrategy>> mergeStrategies() {
		return strategyRegistry.aliases();
	}

	public List<MergeTypeAdapter> typeAdapters() {
		return adapterRegistry.asList();
	}

	public List<MergePolicyResolver> policyResolvers() {
		return policyResolverRegistry.asList();
	}


	public Class<? extends FieldMergeStrategy> defaultStrategy() {
		return defaultStrategy;
	}

	String defaultStrategyName() {
		return defaultStrategyName;
	}

	public MergeBehavior mergeBehavior() {
		return mergeBehavior;
	}


	private Path resolve(String file) {
		return FileUtil.resolvePathWithExtension(file, extension);
	}

	private Path resolve(Path path) {
		return FileUtil.resolvePathWithExtension(path, extension);
	}

	private JsonNode readTree(Path path) {
		if (path == null) throw new ConfigException("path must not be null");

		Path target = resolve(path);
		if (!Files.exists(target))
			throw new ConfigException("Config file does not exist: " + target);

		try (BufferedReader reader = Files.newBufferedReader(target)) {
			JsonNode node = mapper.readTree(reader);
			if (node == null) throw new ConfigException("Config file is empty or invalid: " + target);
			return node;
		} catch (IOException e) {
			throw new ConfigException("Failed to read config tree: " + target, e);
		}
	}

	private JsonNode readTree(byte[] bytes) {
		if (bytes == null || bytes.length == 0)
			return mapper.createObjectNode();

		try {
			JsonNode node = mapper.readTree(bytes);
			return node != null ? node : mapper.createObjectNode();
		} catch (IOException e) {
			throw new ConfigException("Failed to read config tree from bytes", e);
		}
	}

	private JsonNode readTree(InputStream inputStream) {
		if (inputStream == null)
			return mapper.createObjectNode();

		try {
			JsonNode node = mapper.readTree(inputStream);
			return node != null ? node : mapper.createObjectNode();
		} catch (IOException e) {
			throw new ConfigException("Failed to read config tree from stream", e);
		}
	}




	private <T> JsonNode existingResolvedTree(Path path, Class<T> type) {
		Path target = resolve(path);
		if (!Files.exists(target))
			return mapper.createObjectNode();

		return readTree(target);
	}

	private <T> T instantiate(Class<T> type) {
		try {
			Class<?> effectiveType = documentProcessor.resolveType(type, null);
			return (T) mapper.treeToValue(mapper.createObjectNode(), effectiveType);
		} catch (Exception e) {
			throw new ConfigException("Failed to instantiate config type " + type.getName(), e);
		}
	}

	private <T> T bind(JsonNode node, Class<T> type, String failureMessage) {
		try {
			Class<?> effectiveType = documentProcessor.resolveType(
					type,
					new DocumentTypeContext(node != null ? node : mapper.createObjectNode(), null, null, null, null, null)
			);
			T value = (T) mapper.treeToValue(node != null ? node : mapper.createObjectNode(), effectiveType);
			documentProcessor.afterBind(value);
			return value;
		} catch (Exception e) {
			throw new ConfigException(failureMessage, e);
		}
	}





	private <T> ObjectNode mergeDefaults(JsonNode source, T model, Class<T> type) {
		return mergeEngine.mergeDefaults(source, model, type);
	}

	private <T> ObjectNode mergeUserModel(JsonNode source, T model, Class<T> type) {
		return mergeEngine.mergeUserModel(source, model, type);
	}
}
