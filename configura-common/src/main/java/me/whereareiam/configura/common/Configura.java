package me.whereareiam.configura.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.common.merge.ConfigMerger;
import me.whereareiam.configura.common.merge.MergePolicyResolver;
import me.whereareiam.configura.common.processor.PostProcessor;
import me.whereareiam.configura.common.template.DefaultTemplateRegistry;
import me.whereareiam.configura.common.template.TemplateSeeder;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergePolicy;
import me.whereareiam.configura.merge.MergePolicyRegistry;
import me.whereareiam.configura.type.MergePreset;

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
	private final DefaultTemplateRegistry templateRegistry;
	private final MergePolicyRegistry policyRegistry;
	private final MergePolicy defaultPolicy;
	private final ObjectMapper mapper;
	private final MergePolicyResolver mergePolicyResolver;
	private final TemplateSeeder updateSeeder;
	private final TemplateSeeder saveSeeder;

	public Configura(
			String extension,
			Function<List<Module>, ObjectMapper> mapperFactory,
			List<Module> modules,
			DefaultTemplateRegistry templateRegistry,
			MergePolicyRegistry policyRegistry,
			MergePolicy defaultPolicy
	) {
		this.extension = extension;
		this.mapperFactory = mapperFactory;
		this.modules = List.copyOf(modules);
		this.templateRegistry = templateRegistry.copy();
		this.policyRegistry = policyRegistry != null ? policyRegistry.copy() : MergePolicyRegistry.standard();
		this.defaultPolicy = defaultPolicy != null ? defaultPolicy : MergePreset.DEEP_DEFAULTS.policy();
		this.mapper = mapperFactory.apply(this.modules);

		this.mergePolicyResolver = new MergePolicyResolver(this.defaultPolicy, this.policyRegistry);
		this.updateSeeder = new TemplateSeeder(this.mapper, this.templateRegistry, TemplateSeeder.SeedingMode.DEFAULT_INSTANCE, this.mergePolicyResolver);
		this.saveSeeder = new TemplateSeeder(this.mapper, this.templateRegistry, TemplateSeeder.SeedingMode.USER_MODEL, this.mergePolicyResolver);
	}

	public ObjectMapper mapper() {
		return mapper;
	}

	public Configura withModule(Module module) {
		List<Module> next = new ArrayList<>(modules);
		if (module != null)
			next.add(module);
		return new Configura(extension, mapperFactory, next, templateRegistry, policyRegistry, defaultPolicy);
	}

	public <T, P extends TemplateProvider<T>> Configura withTemplate(Class<P> providerClass) {
		DefaultTemplateRegistry registry = templateRegistry.copy();
		registry.registerTemplate(providerClass);
		return new Configura(extension, mapperFactory, modules, registry, policyRegistry, defaultPolicy);
	}

	public Configura withMergePolicy(String name, MergePolicy policy) {
		MergePolicyRegistry next = policyRegistry.copy();
		next.register(name, policy);
		return new Configura(extension, mapperFactory, modules, templateRegistry, next, defaultPolicy);
	}

	public Configura withDefaultMergePolicy(MergePolicy defaultPolicy) {
		return new Configura(extension, mapperFactory, modules, templateRegistry, policyRegistry, defaultPolicy);
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

		try {
			T value = mapper.readValue(target.toFile(), type);
			PostProcessor.process(value);
			return value;
		} catch (IOException e) {
			throw new ConfigException("Failed to read config file: " + target, e);
		}
	}

	public <T> T read(byte[] bytes, Class<T> type) {
		if (type == null) throw new ConfigException("type must not be null");
		try {
			T value = (bytes == null || bytes.length == 0)
					? mapper.treeToValue(mapper.createObjectNode(), type)
					: mapper.readValue(bytes, type);
			PostProcessor.process(value);
			return value;
		} catch (Exception e) {
			throw new ConfigException("Failed to read config bytes for " + type.getName(), e);
		}
	}

	public <T> T read(InputStream inputStream, Class<T> type) {
		if (type == null) throw new ConfigException("type must not be null");
		try {
			T value = inputStream == null
					? mapper.treeToValue(mapper.createObjectNode(), type)
					: mapper.readValue(inputStream, type);
			PostProcessor.process(value);
			return value;
		} catch (Exception e) {
			throw new ConfigException("Failed to read config stream for " + type.getName(), e);
		}
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

	public <T> void write(String file, T value) {
		write(resolve(file), value);
	}

	public <T> void write(Path path, T value) {
		Path target = resolve(path);
		try {
			ensureParent(target);
			mapper.writeValue(target.toFile(), value);
		} catch (IOException e) {
			throw new ConfigException("Failed to write config file: " + target, e);
		}
	}

	public <T> void save(String file, T value) {
		save(resolve(file), value);
	}

	public <T> void save(Path path, T value) {
		Path target = resolve(path);
		try {
			ensureParent(target);
			T seeded = saveSeeder.seed(value);
			ObjectNode merged = ConfigMerger.buildMergedNodeFavorModel(target, seeded, mapper, mergePolicyResolver);
			mapper.writeValue(target.toFile(), merged);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + target, e);
		}
	}

	public <T> byte[] writeBytes(T value) {
		try {
			return mapper.writeValueAsBytes(value);
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
		T seeded = updateSeeder.seed(value);
		ObjectNode merged = ConfigMerger.buildMergedNodeFavorExisting(target, seeded, mapper, mergePolicyResolver);
		return bind(merged, (Class<T>) seeded.getClass());
	}

	public <T> T update(String file, Class<T> type) {
		return update(resolve(file), type);
	}

	public <T> T update(Path path, Class<T> type) {
		Path target = resolve(path);
		T empty = instantiate(type);
		T seeded = updateSeeder.seed(empty);
		ObjectNode merged = ConfigMerger.buildMergedNodeFavorExisting(target, seeded, mapper, mergePolicyResolver);
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

	private <T> T instantiate(Class<T> type) {
		try {
			return mapper.treeToValue(mapper.createObjectNode(), type);
		} catch (Exception e) {
			throw new ConfigException("Failed to instantiate config type " + type.getName(), e);
		}
	}

	private <T> T bind(ObjectNode node, Class<T> type) {
		try {
			T value = mapper.treeToValue(node, type);
			PostProcessor.process(value);
			return value;
		} catch (Exception e) {
			throw new ConfigException("Failed to bind merged config to " + type.getName(), e);
		}
	}

	public String extension() {
		return extension;
	}

	public List<Module> modules() {
		return modules;
	}

	public DefaultTemplateRegistry templateRegistry() {
		return templateRegistry.copy();
	}

	public Map<String, MergePolicy> policies() {
		return policyRegistry.asMap();
	}

	public MergePolicy defaultPolicy() {
		return defaultPolicy;
	}
}
