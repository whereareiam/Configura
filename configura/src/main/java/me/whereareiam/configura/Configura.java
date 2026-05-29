package me.whereareiam.configura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.MergeEngine;
import me.whereareiam.configura.common.merge.defaults.MergeDefaultsProviderRegistry;
import me.whereareiam.configura.common.migration.MigrationDefinitionRegistry;
import me.whereareiam.configura.common.migration.SchemaMigrationEngine;
import me.whereareiam.configura.common.processor.PostProcessor;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;
import me.whereareiam.configura.merge.plugin.MergePlugin;
import me.whereareiam.configura.merge.plugin.MergePluginRegistry;
import me.whereareiam.configura.merge.policy.MergePolicyResolver;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategyRegistry;
import me.whereareiam.configura.migration.MigrationDefinition;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Configura {
	private final String extension;

	private final Function<List<Module>, ObjectMapper> mapperFactory;
	private final List<Module> modules;

	private final MergeDefaultsProviderRegistry defaultProviderRegistry;
	private final FieldMergeStrategyRegistry strategyRegistry;
	private final MergePluginRegistry pluginRegistry;
	private final MergePolicyResolverRegistry policyResolverRegistry;
	private final MigrationDefinitionRegistry versionedRegistry;

	private final Class<? extends FieldMergeStrategy> defaultStrategy;
	private final MergeBehavior mergeBehavior;
	private final boolean backupOnMigration;

	private final ObjectMapper mapper;
	private final SchemaMigrationEngine migrationRunner;
	private final MergeEngine mergeEngine;
	private final ConfigReader reader;
	private final ConfigWriter writer;

	Configura(
			String extension,

			Function<List<Module>, ObjectMapper> mapperFactory,
			List<Module> modules,

			MergeDefaultsProviderRegistry defaultProviderRegistry,
			FieldMergeStrategyRegistry strategyRegistry,
			MergePluginRegistry pluginRegistry,
			MergePolicyResolverRegistry policyResolverRegistry,
			MigrationDefinitionRegistry versionedRegistry,

			Class<? extends FieldMergeStrategy> defaultStrategy,
			MergeBehavior mergeBehavior,
			boolean backupOnMigration
	) {
		this.extension = extension;

		this.mapperFactory = mapperFactory;
		this.modules = List.copyOf(modules);

		this.defaultProviderRegistry = defaultProviderRegistry.copy();
		this.strategyRegistry = strategyRegistry.copy();
		this.pluginRegistry = pluginRegistry.copy();
		this.policyResolverRegistry = policyResolverRegistry.copy();
		this.versionedRegistry = versionedRegistry.copy();

		this.defaultStrategy = defaultStrategy;
		this.mergeBehavior = mergeBehavior != null ? mergeBehavior : MergeBehavior.defaults();
		this.backupOnMigration = backupOnMigration;

		this.mapper = mapperFactory.apply(this.modules);
		this.migrationRunner = new SchemaMigrationEngine(this.mapper, this.versionedRegistry);
		this.mergeEngine = new MergeEngine(
				this.mapper,
				this.defaultProviderRegistry,
				this.strategyRegistry,
				this.pluginRegistry,
				this.policyResolverRegistry,
				this.defaultStrategy,
				this.mergeBehavior
		);

		this.reader = new DefaultConfigReader(this.extension, this.mapper);
		this.writer = new DefaultConfigWriter(this.extension, this.mapper);
	}

	public ObjectMapper mapper() {
		return mapper;
	}

	Function<List<Module>, ObjectMapper> mapperFactory() {
		return mapperFactory;
	}

	FieldMergeStrategyRegistry strategyRegistry() {
		return strategyRegistry.copy();
	}

	MergePluginRegistry pluginRegistry() {
		return pluginRegistry.copy();
	}

	MergePolicyResolverRegistry policyResolverRegistry() {
		return policyResolverRegistry.copy();
	}

	MigrationDefinitionRegistry versionedRegistry() {
		return versionedRegistry.copy();
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

		SchemaMigrationEngine.MigrationResult resolved = readResolvedTreeInternal(resolve(path), type);
		return bind(resolved.node(), type, "Failed to bind config to " + type.getName());
	}

	public <T> T read(byte[] bytes, Class<T> type) {
		if (type == null) throw new ConfigException("configClass must not be null");

		SchemaMigrationEngine.MigrationResult resolved = readResolvedTreeInternal(bytes, type);
		return bind(resolved.node(), type, "Failed to read config bytes for " + type.getName());
	}

	public <T> T read(InputStream inputStream, Class<T> type) {
		if (type == null) throw new ConfigException("configClass must not be null");

		SchemaMigrationEngine.MigrationResult resolved = readResolvedTreeInternal(inputStream, type);
		return bind(resolved.node(), type, "Failed to read config stream for " + type.getName());
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

		writer.writeNode(path, versionedNode(value));
	}

	public <T> void save(String file, T value) {
		if (file == null) throw new NullPointerException("file");
		save(Path.of(file), value);
	}

	@SuppressWarnings("unchecked")
	public <T> void save(Path path, T value) {
		if (value == null) throw new NullPointerException("value");

		Class<T> type = (Class<T>) value.getClass();
		SchemaMigrationEngine.MigrationResult existing = existingResolvedTree(path, type);
		backupBeforePersistedMigration(path, existing);

		ObjectNode source = mapper.valueToTree(value);
		ObjectNode merged = mergeUserModel(source, value, type);
		stampCurrentVersion(type, merged, existing.node());
		writer.writeNode(path, merged);
	}

	public <T> byte[] writeBytes(T value) {
		if (value == null)
			return writer.writeBytes(null);

		return writer.writeBytes(versionedNode(value));
	}

	public <T> T merge(String file, T value) {
		return merge(resolve(file), value);
	}

	@SuppressWarnings("unchecked")
	public <T> T merge(Path path, T value) {
		if (value == null) throw new ConfigException("config must not be null");

		Class<T> type = (Class<T>) value.getClass();
		SchemaMigrationEngine.MigrationResult existing = existingResolvedTree(path, type);
		ObjectNode merged = mergeDefaults(existing.node(), value, type);
		stampCurrentVersion(type, merged, existing.node());
		return bind(merged, type, "Failed to bind merged config to " + type.getName());
	}

	public <T> T update(String file, Class<T> type) {
		return update(Path.of(file), type);
	}

	public <T> T update(Path path, Class<T> type) {
		T empty = instantiate(type);
		SchemaMigrationEngine.MigrationResult existing = existingResolvedTree(path, type);
		backupBeforePersistedMigration(path, existing);

		ObjectNode merged = mergeDefaults(existing.node(), empty, type);
		stampCurrentVersion(type, merged, existing.node());
		writer.writeNode(path, merged);
		return bind(merged, type, "Failed to bind updated config to " + type.getName());
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
		return readResolvedTreeInternal(resolve(path), type).node();
	}

	public <T> JsonNode readResolvedNode(byte[] bytes, Class<T> type) {
		return readResolvedTreeInternal(bytes, type).node();
	}

	public <T> JsonNode readResolvedNode(InputStream inputStream, Class<T> type) {
		return readResolvedTreeInternal(inputStream, type).node();
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
		return new Configura(extension, mapperFactory, next, defaultProviderRegistry, strategyRegistry, pluginRegistry, policyResolverRegistry, versionedRegistry, defaultStrategy, mergeBehavior, backupOnMigration);
	}

	public <T, P extends MergeDefaultsProvider<T>> Configura withDefaults(Class<P> providerClass) {
		MergeDefaultsProviderRegistry registry = defaultProviderRegistry.copy();
		registry.registerProvider(providerClass);
		return new Configura(extension, mapperFactory, modules, registry, strategyRegistry, pluginRegistry, policyResolverRegistry, versionedRegistry, defaultStrategy, mergeBehavior, backupOnMigration);
	}

	public Configura withStrategy(String name, Class<? extends FieldMergeStrategy> strategy) {
		FieldMergeStrategyRegistry next = strategyRegistry.copy();
		next.register(name, strategy);
		return new Configura(extension, mapperFactory, modules, defaultProviderRegistry, next, pluginRegistry, policyResolverRegistry, versionedRegistry, defaultStrategy, mergeBehavior, backupOnMigration);
	}

	public Configura withPlugin(MergePlugin plugin) {
		MergePluginRegistry next = pluginRegistry.copy();
		next.register(plugin);
		return new Configura(extension, mapperFactory, modules, defaultProviderRegistry, strategyRegistry, next, policyResolverRegistry, versionedRegistry, defaultStrategy, mergeBehavior, backupOnMigration);
	}

	public Configura withPolicyResolver(MergePolicyResolver resolver) {
		MergePolicyResolverRegistry next = policyResolverRegistry.copy();
		next.register(resolver);
		return new Configura(extension, mapperFactory, modules, defaultProviderRegistry, strategyRegistry, pluginRegistry, next, versionedRegistry, defaultStrategy, mergeBehavior, backupOnMigration);
	}

	public Configura withDefaultStrategy(Class<? extends FieldMergeStrategy> strategy) {
		return new Configura(extension, mapperFactory, modules, defaultProviderRegistry, strategyRegistry, pluginRegistry, policyResolverRegistry, versionedRegistry, strategy, mergeBehavior, backupOnMigration);
	}

	public Configura withMergeBehavior(MergeBehavior mergeBehavior) {
		return new Configura(extension, mapperFactory, modules, defaultProviderRegistry, strategyRegistry, pluginRegistry, policyResolverRegistry, versionedRegistry, defaultStrategy, mergeBehavior, backupOnMigration);
	}

	public Configura withBackupOnMigration(boolean backupOnMigration) {
		return new Configura(extension, mapperFactory, modules, defaultProviderRegistry, strategyRegistry, pluginRegistry, policyResolverRegistry, versionedRegistry, defaultStrategy, mergeBehavior, backupOnMigration);
	}

	public <T> Configura withVersioned(Class<T> type, Consumer<MigrationDefinition<T>> customizer) {
		MigrationDefinition<T> definition = new MigrationDefinition<>(type);
		if (customizer != null)
			customizer.accept(definition);
		MigrationDefinitionRegistry next = versionedRegistry.copy();
		next.register(definition);
		return new Configura(extension, mapperFactory, modules, defaultProviderRegistry, strategyRegistry, pluginRegistry, policyResolverRegistry, next, defaultStrategy, mergeBehavior, backupOnMigration);
	}

	public String extension() {
		return extension;
	}

	public List<Module> modules() {
		return modules;
	}

	public MergeDefaultsProviderRegistry registeredDefaultProviders() {
		return defaultProviderRegistry.copy();
	}

	public Map<String, Class<? extends FieldMergeStrategy>> mergeStrategies() {
		return strategyRegistry.asMap();
	}

	public List<MergePlugin> mergePlugins() {
		return pluginRegistry.asList();
	}

	public List<MergePolicyResolver> policyResolvers() {
		return policyResolverRegistry.asList();
	}

	public boolean isVersioned(Class<?> type) {
		return versionedRegistry.contains(type);
	}

	public Class<? extends FieldMergeStrategy> defaultStrategy() {
		return defaultStrategy;
	}

	public MergeBehavior mergeBehavior() {
		return mergeBehavior;
	}

	public boolean backupOnMigration() {
		return backupOnMigration;
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

	private <T> SchemaMigrationEngine.MigrationResult readResolvedTreeInternal(Path path, Class<T> type) {
		return migrationRunner.migrate(type, readTree(path), path, false);
	}

	private <T> SchemaMigrationEngine.MigrationResult readResolvedTreeInternal(byte[] bytes, Class<T> type) {
		return migrationRunner.migrate(type, readTree(bytes), null, bytes == null || bytes.length == 0);
	}

	private <T> SchemaMigrationEngine.MigrationResult readResolvedTreeInternal(
			InputStream inputStream,
			Class<T> type
	) {
		return migrationRunner.migrate(type, readTree(inputStream), null, inputStream == null);
	}

	private <T> SchemaMigrationEngine.MigrationResult existingResolvedTree(Path path, Class<T> type) {
		Path target = resolve(path);
		if (!Files.exists(target))
			return migrationRunner.migrate(type, mapper.createObjectNode(), target, true);

		return readResolvedTreeInternal(target, type);
	}

	private <T> T instantiate(Class<T> type) {
		try {
			return mapper.treeToValue(mapper.createObjectNode(), type);
		} catch (Exception e) {
			throw new ConfigException("Failed to instantiate config type " + type.getName(), e);
		}
	}

	private <T> T bind(JsonNode node, Class<T> type, String failureMessage) {
		try {
			T value = mapper.treeToValue(node != null ? node : mapper.createObjectNode(), type);
			PostProcessor.process(value);
			return value;
		} catch (Exception e) {
			throw new ConfigException(failureMessage, e);
		}
	}

	private <T> ObjectNode stampCurrentVersion(Class<T> type, ObjectNode node) {
		return migrationRunner.stampCurrentVersion(type, node);
	}

	private <T> ObjectNode stampCurrentVersion(Class<T> type, ObjectNode node, JsonNode source) {
		return migrationRunner.stampCurrentVersion(type, node, source);
	}

	@SuppressWarnings("unchecked")
	private <T> ObjectNode versionedNode(T value) {
		ObjectNode node = mapper.valueToTree(value);
		return stampCurrentVersion((Class<T>) value.getClass(), node);
	}

	private void backupBeforePersistedMigration(Path path, SchemaMigrationEngine.MigrationResult existing) {
		Path target = resolve(path);
		if (!backupOnMigration || existing == null || !existing.migrated() || !Files.exists(target))
			return;

		Path backup = target.resolveSibling(target.getFileName().toString() + ".bak");
		try {
			Files.createDirectories(backup.getParent() != null ? backup.getParent() : Path.of("."));
			Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new ConfigException("Failed to back up config file before migration: " + target, e);
		}
	}

	private <T> ObjectNode mergeDefaults(JsonNode source, T model, Class<T> type) {
		return mergeEngine.mergeDefaults(source, model, type);
	}

	private <T> ObjectNode mergeUserModel(JsonNode source, T model, Class<T> type) {
		return mergeEngine.mergeUserModel(source, model, type);
	}
}
