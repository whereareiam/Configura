package me.whereareiam.configura.common.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.common.AdapterRegistry;
import me.whereareiam.configura.common.serialization.MapperFactory;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultConfigReader implements ConfigReader {
	private final Format format;
	private final AdapterRegistry registry;
	private final ObjectMapper mapper;

	public DefaultConfigReader() {
		this.format = Format.YAML;
		this.registry = AdapterRegistry.empty();
		this.mapper = MapperFactory.buildMapper(this.format, this.registry);
	}

	@Override
	public ConfigReader withFormat(Format format) {
		return new DefaultConfigReader(format, this.registry);
	}

	@Override
	public <T> ConfigReader registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		AdapterRegistry nextRegistry = this.registry.withAdapter(type, adapterClass);
		return new DefaultConfigReader(this.format, nextRegistry);
	}

	@Override
	public Format getFormat() {
		return format;
	}

	@Override
	public <T> T load(String filePath, Class<T> configClass) {
		String resolved = FileUtil.resolvePathWithFormat(filePath, format);
		ObjectNode sourceNode = mapper.createObjectNode();
		Path path = Path.of(resolved);
		if (Files.exists(path)) {
			try {
				JsonNode node = mapper.readTree(Files.newBufferedReader(path));
				if (node != null && node.isObject()) {
					sourceNode = (ObjectNode) node;
				}
			} catch (IOException e) {
				throw new ConfigException("Failed to read config file: " + resolved, e);
			}
		}

		T instance;
		try {
			instance = mapper.treeToValue(sourceNode, configClass);
		} catch (Exception e) {
			throw new ConfigException("Failed to bind config to " + configClass.getName(), e);
		}

		return instance;
	}

	@Override
	public boolean exists(String filePath) {
		String resolved = FileUtil.resolvePathWithFormat(filePath, format);
		return Files.exists(Path.of(resolved));
	}

	private DefaultConfigReader(Format format, AdapterRegistry registry) {
		this.format = format;
		this.registry = registry;
		this.mapper = MapperFactory.buildMapper(this.format, this.registry);
	}
}


