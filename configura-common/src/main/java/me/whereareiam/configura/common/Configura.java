package me.whereareiam.configura.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.DefaultMergeDefaultsRegistry;
import me.whereareiam.configura.common.merge.MergeEngine;
import me.whereareiam.configura.common.migration.MigrationDefinitionRegistry;
import me.whereareiam.configura.common.migration.SchemaMigrationEngine;
import me.whereareiam.configura.common.processor.PostProcessor;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergeDefaultsProvider;
import me.whereareiam.configura.merge.MergeStrategy;
import me.whereareiam.configura.merge.MergeStrategyRegistry;
import me.whereareiam.configura.merge.strategy.DeepDefaults;
import me.whereareiam.configura.migration.MigrationDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Configura {
	private final String extension;
	private final Function<List<Module>, ObjectMapper> mapperFactory;
	private final List<Module> modules;
	private final DefaultMergeDefaultsRegistry defaultsRegistry;
	private final MergeStrategyRegistry strategyRegistry;
	private final MigrationDefinitionRegistry versionedRegistry;
	private final Class<? extends MergeStrategy> defaultStrategy;
	private final boolean backupOnMigration;
	private final ObjectMapper mapper;
	private final SchemaMigrationEngine migrationRunner;
	private final MergeEngine mergeEngine;

	public Configura(
			String extension,
			Function<List<Module>, ObjectMapper> mapperFactory,
			List<Module> modules,
			DefaultMergeDefaultsRegistry defaultsRegistry,
			MergeStrategyRegistry strategyRegistry,
			MigrationDefinitionRegistry versionedRegistry,
			Class<? extends MergeStrategy> defaultStrategy,
			boolean backupOnMigration
	) {
		this.extension = extension;
		this.mapperFactory = mapperFactory;
		this.modules = List.copyOf(modules);
		this.defaultsRegistry = defaultsRegistry != null ? defaultsRegistry.copy() : new DefaultMergeDefaultsRegistry();
		this.strategyRegistry = strategyRegistry != null ? strategyRegistry.copy() : MergeStrategyRegistry.standard();
		this.versionedRegistry = versionedRegistry != null ? versionedRegistry.copy() : new MigrationDefinitionRegistry();
		this.defaultStrategy = defaultStrategy != null ? defaultStrategy : DeepDefaults.class;
		this.backupOnMigration = backupOnMigration;
		this.mapper = mapperFactory.apply(this.modules);
		this.migrationRunner = new SchemaMigrationEngine(this.mapper, this.versionedRegistry);
		this.mergeEngine = new MergeEngine(this.mapper, this.defaultsRegistry, this.strategyRegistry, this.defaultStrategy);
	}

	public ObjectMapper mapper() {
		return mapper;
	}

	public Configura withModule(Module module) {
		List<Module> next = new ArrayList<>(modules);
		if (module != null)
			next.add(module);
		return new Configura(extension, mapperFactory, next, defaultsRegistry, strategyRegistry, versionedRegistry, defaultStrategy, backupOnMigration);
	}

	public <T, P extends MergeDefaultsProvider<T>> Configura withDefaults(Class<P> providerClass) {
		DefaultMergeDefaultsRegistry registry = defaultsRegistry.copy();
		registry.registerDefaults(providerClass);
		return new Configura(extension, mapperFactory, modules, registry, strategyRegistry, versionedRegistry, defaultStrategy, backupOnMigration);
	}

	public Configura withMergeStrategy(String name, Class<? extends MergeStrategy> strategy) {
		MergeStrategyRegistry next = strategyRegistry.copy();
		next.register(name, strategy);
		return new Configura(extension, mapperFactory, modules, defaultsRegistry, next, versionedRegistry, defaultStrategy, backupOnMigration);
	}

	public Configura withDefaultMergeStrategy(Class<? extends MergeStrategy> defaultStrategy) {
		return new Configura(extension, mapperFactory, modules, defaultsRegistry, strategyRegistry, versionedRegistry, defaultStrategy, backupOnMigration);
	}

	public Configura withBackupOnMigration(boolean backupOnMigration) {
		return new Configura(extension, mapperFactory, modules, defaultsRegistry, strategyRegistry, versionedRegistry, defaultStrategy, backupOnMigration);
	}

	public <T> Configura withVersioned(MigrationDefinition<T> definition) {
		MigrationDefinitionRegistry next = versionedRegistry.copy();
		next.register(definition);
		return new Configura(extension, mapperFactory, modules, defaultsRegistry, strategyRegistry, next, defaultStrategy, backupOnMigration);
	}

	public <T> T read(String file, Class<T> type) {
		return read(resolve(file), type);
	}

	public <T> T read(Path path, Class<T> type) {
		if (path == null) throw new ConfigException("path must not be null");
		if (type == null) throw new ConfigException("type must not be null");

		Path target = resolve(path);
		if (!Files.exists(target))
			throw new ConfigException("Config file does not exist: " + target);

		SchemaMigrationEngine.MigrationResult migrated = readMigratedTreeInternal(target, type);
		return bind(migrated.node(), type, "Failed to bind config to " + type.getName());
	}

	public <T> T read(byte[] bytes, Class<T> type) {
		if (type == null) throw new ConfigException("type must not be null");
		SchemaMigrationEngine.MigrationResult migrated = readMigratedTreeInternal(bytes, type);
		return bind(migrated.node(), type, "Failed to read config bytes for " + type.getName());
	}

	public <T> T read(InputStream inputStream, Class<T> type) {
		if (type == null) throw new ConfigException("type must not be null");
		SchemaMigrationEngine.MigrationResult migrated = readMigratedTreeInternal(inputStream, type);
		return bind(migrated.node(), type, "Failed to read config stream for " + type.getName());
	}

	public JsonNode readTree(String file) {
		return readTree(resolve(file));
	}

	public JsonNode readTree(Path path) {
		if (path == null) throw new ConfigException("path must not be null");
		Path target = resolve(path);
		if (!Files.exists(target))
			throw new ConfigException("Config file does not exist: " + target);

		try {
			JsonNode node = mapper.readTree(target.toFile());
			if (node == null) throw new ConfigException("Config file is empty or invalid: " + target);
			return node;
		} catch (IOException e) {
			throw new ConfigException("Failed to read config tree: " + target, e);
		}
	}

	public JsonNode readTree(byte[] bytes) {
		if (bytes == null || bytes.length == 0)
			return mapper.createObjectNode();
		try {
			JsonNode node = mapper.readTree(bytes);
			return node != null ? node : mapper.createObjectNode();
		} catch (IOException e) {
			throw new ConfigException("Failed to read config tree from bytes", e);
		}
	}

	public JsonNode readTree(InputStream inputStream) {
		if (inputStream == null)
			return mapper.createObjectNode();
		try {
			JsonNode node = mapper.readTree(inputStream);
			return node != null ? node : mapper.createObjectNode();
		} catch (IOException e) {
			throw new ConfigException("Failed to read config tree from stream", e);
		}
	}

	public <T> JsonNode readMigratedTree(String file, Class<T> type) {
		return readMigratedTree(resolve(file), type);
	}

	public <T> JsonNode readMigratedTree(Path path, Class<T> type) {
		return readMigratedTreeInternal(resolve(path), type).node();
	}

	public <T> JsonNode readMigratedTree(byte[] bytes, Class<T> type) {
		return readMigratedTreeInternal(bytes, type).node();
	}

	public <T> JsonNode readMigratedTree(InputStream inputStream, Class<T> type) {
		return readMigratedTreeInternal(inputStream, type).node();
	}

	public <T> void write(String file, T value) {
		write(resolve(file), value);
	}

	public <T> void write(Path path, T value) {
		Path target = resolve(path);
		try {
			ensureParent(target);
			mapper.writeValue(target.toFile(), value == null ? null : versionedNode(value));
		} catch (IOException e) {
			throw new ConfigException("Failed to write config file: " + target, e);
		}
	}

	public <T> void save(String file, T value) {
		save(resolve(file), value);
	}

	@SuppressWarnings("unchecked")
	public <T> void save(Path path, T value) {
		Path target = resolve(path);
		try {
			ensureParent(target);
			SchemaMigrationEngine.MigrationResult existing = existingMigratedTree(target, (Class<T>) value.getClass());
			backupBeforePersistedMigration(target, existing);
			ObjectNode source = mapper.valueToTree(value);
			ObjectNode merged = mergeEngine.merge(source, value, (Class<T>) value.getClass(), MergeEngine.Mode.USER_MODEL);
			migrationRunner.stampCurrentVersion((Class<T>) value.getClass(), merged, existing.node());
			mapper.writeValue(target.toFile(), merged);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + target, e);
		}
	}

	public <T> byte[] writeBytes(T value) {
		try {
			return mapper.writeValueAsBytes(value == null ? null : versionedNode(value));
		} catch (IOException e) {
			throw new ConfigException("Failed to write config bytes", e);
		}
	}

	public void writeTree(String file, JsonNode node) {
		writeTree(resolve(file), node);
	}

	public void writeTree(Path path, JsonNode node) {
		Path target = resolve(path);
		try {
			ensureParent(target);
			mapper.writeValue(target.toFile(), node != null ? node : mapper.createObjectNode());
		} catch (IOException e) {
			throw new ConfigException("Failed to write config tree: " + target, e);
		}
	}

	public byte[] writeTreeBytes(JsonNode node) {
		try {
			return mapper.writeValueAsBytes(node != null ? node : mapper.createObjectNode());
		} catch (IOException e) {
			throw new ConfigException("Failed to write config tree bytes", e);
		}
	}

	public <T> T merge(String file, T value) {
		return merge(resolve(file), value);
	}

	@SuppressWarnings("unchecked")
	public <T> T merge(Path path, T value) {
		Path target = resolve(path);
		SchemaMigrationEngine.MigrationResult existing = existingMigratedTree(target, (Class<T>) value.getClass());
		ObjectNode merged = mergeEngine.merge(existing.node(), value, (Class<T>) value.getClass(), MergeEngine.Mode.DEFAULT_INSTANCE);
		migrationRunner.stampCurrentVersion((Class<T>) value.getClass(), merged, existing.node());
		return bind(merged, (Class<T>) value.getClass(), "Failed to bind merged config to " + value.getClass().getName());
	}

	public <T> T update(String file, Class<T> type) {
		return update(resolve(file), type);
	}

	public <T> T update(Path path, Class<T> type) {
		Path target = resolve(path);
		T empty = instantiate(type);
		SchemaMigrationEngine.MigrationResult existing = existingMigratedTree(target, type);
		backupBeforePersistedMigration(target, existing);
		ObjectNode merged = mergeEngine.merge(existing.node(), empty, type, MergeEngine.Mode.DEFAULT_INSTANCE);
		migrationRunner.stampCurrentVersion(type, merged, existing.node());
		writeTree(target, merged);
		return read(target, type);
	}

	private Path resolve(String file) {
		return FileUtil.resolvePathWithExtension(file, extension);
	}

	private Path resolve(Path path) {
		return FileUtil.resolvePathWithExtension(path, extension);
	}

	private void ensureParent(Path path) throws IOException {
		Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
	}

	private void backupBeforePersistedMigration(Path path, SchemaMigrationEngine.MigrationResult existing) {
		if (!backupOnMigration || existing == null || !existing.migrated() || !Files.exists(path))
			return;

		Path backup = path.resolveSibling(path.getFileName().toString() + ".bak");
		try {
			ensureParent(backup);
			Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new ConfigException("Failed to back up config file before migration: " + path, e);
		}
	}

	private <T> T instantiate(Class<T> type) {
		try {
			return mapper.treeToValue(mapper.createObjectNode(), type);
		} catch (Exception e) {
			throw new ConfigException("Failed to instantiate config type " + type.getName(), e);
		}
	}

	private <T> T bind(ObjectNode node, Class<T> type, String failureMessage) {
		try {
			T value = mapper.treeToValue(node, type);
			PostProcessor.process(value);
			return value;
		} catch (Exception e) {
			throw new ConfigException(failureMessage, e);
		}
	}

	private <T> SchemaMigrationEngine.MigrationResult readMigratedTreeInternal(Path path, Class<T> type) {
		return migrationRunner.migrate(type, readTree(path), path, false);
	}

	private <T> SchemaMigrationEngine.MigrationResult readMigratedTreeInternal(byte[] bytes, Class<T> type) {
		return migrationRunner.migrate(type, readTree(bytes), null, bytes == null || bytes.length == 0);
	}

	private <T> SchemaMigrationEngine.MigrationResult readMigratedTreeInternal(InputStream inputStream, Class<T> type) {
		return migrationRunner.migrate(type, readTree(inputStream), null, inputStream == null);
	}

	private <T> SchemaMigrationEngine.MigrationResult existingMigratedTree(Path path, Class<T> type) {
		if (!Files.exists(path))
			return migrationRunner.migrate(type, mapper.createObjectNode(), path, true);
		return readMigratedTreeInternal(path, type);
	}

	@SuppressWarnings("unchecked")
	private <T> ObjectNode versionedNode(T value) {
		ObjectNode node = mapper.valueToTree(value);
		return migrationRunner.stampCurrentVersion((Class<T>) value.getClass(), node);
	}

	public String extension() {
		return extension;
	}

	public List<Module> modules() {
		return modules;
	}

	public DefaultMergeDefaultsRegistry defaultsRegistry() {
		return defaultsRegistry.copy();
	}

	public Map<String, Class<? extends MergeStrategy>> strategies() {
		return strategyRegistry.asMap();
	}

	public boolean isVersioned(Class<?> type) {
		return versionedRegistry.contains(type);
	}

	public Class<? extends MergeStrategy> defaultStrategy() {
		return defaultStrategy;
	}

	public boolean backupOnMigration() {
		return backupOnMigration;
	}
}
