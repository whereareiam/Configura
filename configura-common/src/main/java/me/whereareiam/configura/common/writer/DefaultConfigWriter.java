package me.whereareiam.configura.common.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.merge.defaults.DefaultMergeDefaultsRegistry;
import me.whereareiam.configura.common.merge.MergeEngine;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.MergeStrategyRegistry;
import me.whereareiam.configura.merge.strategy.DeepDefaults;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultConfigWriter implements ConfigWriter {
	private final Format format;
	private final ObjectMapper mapper;
	private final MergeEngine mergeEngine;

	public DefaultConfigWriter() {
		this(Format.YAML);
	}

	private DefaultConfigWriter(Format format) {
		this.format = format;
		this.mapper = MapperFactory.buildWriterMapper(this.format);
		this.mergeEngine = new MergeEngine(
				this.mapper,
				new DefaultMergeDefaultsRegistry(),
				MergeStrategyRegistry.standard(),
				DeepDefaults.class
		);
	}

	@Override
	public ConfigWriter withFormat(Format format) {
		return new DefaultConfigWriter(format);
	}

	@Override
	public Format getFormat() {
		return format;
	}

	@Override
	public <T> void encode(String file, T config) {
		encode(FileUtil.resolvePathWithFormat(file, format), config);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> void encode(Path path, T config) {
		Path target = FileUtil.resolvePathWithFormat(path, format);
		try {
			Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
			ObjectNode source = mapper.valueToTree(config);
			ObjectNode toWrite = mergeEngine.merge(source, config, (Class<T>) config.getClass(), MergeEngine.Mode.USER_MODEL);
			mapper.writeValue(target.toFile(), toWrite);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + target, e);
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
		write(FileUtil.resolvePathWithFormat(file, format), config);
	}

	@Override
	public <T> void write(Path path, T config) {
		Path target = FileUtil.resolvePathWithFormat(path, format);
		try {
			Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
			mapper.writeValue(target.toFile(), config);
		} catch (IOException e) {
			throw new ConfigException("Failed to write config file: " + target, e);
		}
	}

	@Override
	public <T> T merge(String file, T config) {
		return merge(FileUtil.resolvePathWithFormat(file, format), config);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T merge(Path path, T config) {
		Path target = FileUtil.resolvePathWithFormat(path, format);
		ObjectNode existing = mapper.createObjectNode();
		if (Files.exists(target)) {
			try {
				existing = (ObjectNode) mapper.readTree(target.toFile());
			} catch (IOException e) {
				throw new ConfigException("Failed to read config file before merge: " + target, e);
			}
		}

		ObjectNode merged = mergeEngine.merge(existing, config, (Class<T>) config.getClass(), MergeEngine.Mode.DEFAULT_INSTANCE);
		return bindNode(merged, (Class<T>) config.getClass());
	}

	private <T> T bindNode(ObjectNode node, Class<T> type) {
		try {
			return mapper.treeToValue(node, type);
		} catch (Exception e) {
			throw new ConfigException("Failed to bind merged node to type: " + type.getName(), e);
		}
	}
}
