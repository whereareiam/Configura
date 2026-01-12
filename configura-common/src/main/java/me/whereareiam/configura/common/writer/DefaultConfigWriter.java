package me.whereareiam.configura.common.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.adapter.AdapterRegistry;
import me.whereareiam.configura.common.node.NodeConverter;
import me.whereareiam.configura.common.merge.ConfigMerger;
import me.whereareiam.configura.common.template.TemplateSeeder;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultConfigWriter implements ConfigWriter {
	private final Format format;
	private final AdapterRegistry registry;
	private final TemplateRegistry templateRegistry;
	private final ObjectMapper mapper;
	private final TemplateSeeder userSeeder;
	private final TemplateSeeder defaultSeeder;

	public DefaultConfigWriter() {
		this(null);
	}

	public DefaultConfigWriter(TemplateRegistry templateRegistry) {
		this.format = Format.YAML;
		this.registry = AdapterRegistry.empty();
		this.templateRegistry = templateRegistry;
		this.mapper = MapperFactory.buildWriterMapper(this.format, this.registry);
		this.userSeeder = new TemplateSeeder(this.mapper, this.templateRegistry, TemplateSeeder.SeedingMode.USER_MODEL);
		this.defaultSeeder = new TemplateSeeder(this.mapper, this.templateRegistry, TemplateSeeder.SeedingMode.DEFAULT_INSTANCE);
	}

	private DefaultConfigWriter(
			Format format,
			AdapterRegistry registry,
			TemplateRegistry templateRegistry
	) {
		this.format = format;
		this.registry = registry;
		this.templateRegistry = templateRegistry;
		this.mapper = MapperFactory.buildWriterMapper(this.format, this.registry);
		this.userSeeder = new TemplateSeeder(this.mapper, this.templateRegistry, TemplateSeeder.SeedingMode.USER_MODEL);
		this.defaultSeeder = new TemplateSeeder(this.mapper, this.templateRegistry, TemplateSeeder.SeedingMode.DEFAULT_INSTANCE);
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

	@Override
	public <T> ConfigWriter registerAdapter(Class<T> type, TypeAdapter<T> adapterInstance) {
		AdapterRegistry next = this.registry.withAdapter(type, adapterInstance);
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

			T seeded = userSeeder.seed(config);
			ObjectNode toWrite = ConfigMerger.buildMergedNodeFavorModel(path, seeded, mapper);

			mapper.writeValue(path.toFile(), toWrite);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + resolved, e);
		}
	}

	@Override
	public <T> void encode(Path path, T config) {
		String resolved = FileUtil.resolvePathWithFormat(path.toString(), format);
		try {
			Path target = Path.of(resolved);
			Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));

			T seeded = userSeeder.seed(config);
			ObjectNode toWrite = ConfigMerger.buildMergedNodeFavorModel(target, seeded, mapper);

			mapper.writeValue(target.toFile(), toWrite);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + resolved, e);
		}
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

		T seeded = defaultSeeder.seed(config);
		ObjectNode merged = ConfigMerger.buildMergedNodeFavorExisting(path, seeded, mapper);
		return bindNode(merged, (Class<T>) seeded.getClass());
	}


	@Override
	public <T> T merge(Path path, T config) {
		String resolved = FileUtil.resolvePathWithFormat(path.toString(), format);
		Path target = Path.of(resolved);

		T seeded = defaultSeeder.seed(config);
		ObjectNode merged = ConfigMerger.buildMergedNodeFavorExisting(target, seeded, mapper);
		return bindNode(merged, (Class<T>) seeded.getClass());
	}

	@Override
	public byte[] encodeNode(Node node) {
		try {
			Node safe = node == null ? new me.whereareiam.configura.node.ObjectNode() : node;
			return mapper.writeValueAsBytes(NodeConverter.toJsonNode(safe));
		} catch (IOException e) {
			throw new ConfigException("Failed to serialize node to bytes", e);
		}
	}

	@Override
	public void writeNode(String file, Node node) {
		String resolved = FileUtil.resolvePathWithFormat(file, format);
		writeNode(Path.of(resolved), node);
	}

	@Override
	public void writeNode(Path path, Node node) {
		String resolved = FileUtil.resolvePathWithFormat(path.toString(), format);
		try {
			Path target = Path.of(resolved);
			Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
			Node safe = node == null ? new me.whereareiam.configura.node.ObjectNode() : node;
			mapper.writeValue(target.toFile(), NodeConverter.toJsonNode(safe));
		} catch (IOException e) {
			throw new ConfigException("Failed to write node config file: " + resolved, e);
		}
	}


	private <T> T bindNode(ObjectNode node, Class<T> type) {
		try {
			return mapper.treeToValue(node, type);
		} catch (Exception e) {
			throw new ConfigException("Failed to bind merged node to type: " + type.getName(), e);
		}
	}
}
