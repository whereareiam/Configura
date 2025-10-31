package me.whereareiam.configura.common.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.annotation.Policy;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.adapter.AdapterRegistry;
import me.whereareiam.configura.common.template.TemplateSeeder;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultConfigWriter implements ConfigWriter {
	private final Format format;
	private final AdapterRegistry registry;
	private final TemplateRegistry templateRegistry;
	private final ObjectMapper mapper;

	public DefaultConfigWriter() {
		this(null);
	}

	public DefaultConfigWriter(TemplateRegistry templateRegistry) {
		this.format = Format.YAML;
		this.registry = AdapterRegistry.empty();
		this.templateRegistry = templateRegistry;
		this.mapper = MapperFactory.buildWriterMapper(this.format, this.registry);
	}

	@Override
	public <T> byte[] encode(T config) {
		try {
			return mapper.writeValueAsBytes(config);
		} catch (IOException e) {
			throw new ConfigException("Failed to serialize config to bytes", e);
		}
	}


	@Override
	public ConfigWriter withFormat(Format format) {
		return new DefaultConfigWriter(format, this.registry, this.templateRegistry);
	}

	@Override
	public <T> ConfigWriter registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		AdapterRegistry next = this.registry.withAdapter(type, adapterClass);
		return new DefaultConfigWriter(this.format, next, this.templateRegistry);
	}

	public ConfigWriter withTemplateRegistry(TemplateRegistry templateRegistry) {
		return new DefaultConfigWriter(this.format, this.registry, templateRegistry);
	}

	@Override
	public Format getFormat() {
		return format;
	}

	@Override
	public <T> void encode(String file, T config) {
		String resolved = FileUtil.resolvePathWithFormat(file, format);
		try {
			Path path = Path.of(resolved);
			Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));

			T seeded = new TemplateSeeder(mapper, templateRegistry).seed(config);
			ObjectNode toWrite = buildMergedNodePreservingPolicy(path, seeded);

			mapper.writeValue(path.toFile(), toWrite);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + resolved, e);
		}
	}

	@Override
	public <T> void write(String file, T config) {
		String resolved = FileUtil.resolvePathWithFormat(file, format);
		try {
			Path path = Path.of(resolved);
			Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
			mapper.writeValue(path.toFile(), config);
		} catch (IOException e) {
			throw new ConfigException("Failed to write config file: " + resolved, e);
		}
	}

	@Override
	public <T> void encode(Path path, T config) {
		String resolved = FileUtil.resolvePathWithFormat(path.toString(), format);
		try {
			Path target = Path.of(resolved);
			Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));

			T seeded = new TemplateSeeder(mapper, templateRegistry).seed(config);
			ObjectNode toWrite = buildMergedNodePreservingPolicy(target, seeded);

			mapper.writeValue(target.toFile(), toWrite);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + resolved, e);
		}
	}

	@Override
	public <T> void write(Path path, T config) {
		String resolved = FileUtil.resolvePathWithFormat(path.toString(), format);
		try {
			Path target = Path.of(resolved);
			Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
			mapper.writeValue(target.toFile(), config);
		} catch (IOException e) {
			throw new ConfigException("Failed to write config file: " + resolved, e);
		}
	}

	@Override
	public <T> T merge(String file, T config) {
		String resolved = FileUtil.resolvePathWithFormat(file, format);
		Path path = Path.of(resolved);

		T seeded = new TemplateSeeder(mapper, templateRegistry).seed(config);
		ObjectNode merged = buildMergedNodePreservingPolicy(path, seeded);
		return bindNode(merged, (Class<T>) seeded.getClass());
	}


	@Override
	public <T> T merge(Path path, T config) {
		String resolved = FileUtil.resolvePathWithFormat(path.toString(), format);
		Path target = Path.of(resolved);

		T seeded = new TemplateSeeder(mapper, templateRegistry).seed(config);
		ObjectNode merged = buildMergedNodePreservingPolicy(target, seeded);
		return bindNode(merged, (Class<T>) seeded.getClass());
	}


	private <T> ObjectNode buildMergedNodePreservingPolicy(Path path, T model) {
		ObjectNode modelNode = toObjectNode(model);
		if (!Files.exists(path)) return modelNode;

		try {
			JsonNode existing = mapper.readTree(path.toFile());
			if (existing != null && existing.isObject()) {
				ObjectNode existingNode = (ObjectNode) existing;
				// Preserve fields marked with @Policy(mergeOnUpdate=false)
				preservePolicyFields(existingNode, modelNode, model.getClass());
			}
		} catch (Exception ignored) {
		}

		return modelNode;
	}

	private static void preservePolicyFields(ObjectNode existingNode, ObjectNode modelNode, Class<?> modelClass) {
		for (Field field : modelClass.getDeclaredFields()) {
			Policy policy = field.getAnnotation(Policy.class);
			if (policy != null && !policy.mergeOnUpdate()) {
				String key = field.getName();
				JsonNode existingVal = existingNode.get(key);
				if (existingVal != null && !existingVal.isNull()) {
					modelNode.set(key, existingVal);
				}
			}
		}
	}

	private ObjectNode toObjectNode(Object model) {
		return mapper.valueToTree(model);
	}

	private <T> T bindNode(ObjectNode node, Class<T> type) {
		try {
			return mapper.treeToValue(node, type);
		} catch (Exception e) {
			throw new ConfigException("Failed to bind merged node to type: " + type.getName(), e);
		}
	}

	private DefaultConfigWriter(Format format, AdapterRegistry registry, TemplateRegistry templateRegistry) {
		this.format = format;
		this.registry = registry;
		this.templateRegistry = templateRegistry;
		this.mapper = MapperFactory.buildWriterMapper(this.format, this.registry);
	}
}