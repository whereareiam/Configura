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

import java.io.BufferedReader;
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

	public <T> T load(String fileName, Class<T> configClass) {
		if (fileName == null) throw new ConfigException("fileName must not be null");
		if (configClass == null) throw new ConfigException("configClass must not be null");

		String resolved = FileUtil.resolvePathWithFormat(fileName, format);

		return load(Path.of(resolved), configClass);
	}

	/**
	 * Load config using an explicit path (directory + filename).
	 */
	public <T> T load(Path path, Class<T> configClass) {
		if (path == null) throw new ConfigException("path must not be null");
		if (configClass == null) throw new ConfigException("configClass must not be null");

		ObjectNode sourceNode = mapper.createObjectNode();

		if (Files.exists(path)) {
			try (BufferedReader reader = Files.newBufferedReader(path)) {
				JsonNode node = mapper.readTree(reader);
				if (node != null && node.isObject())
					sourceNode = (ObjectNode) node;
			} catch (IOException e) {
				throw new ConfigException("Failed to read config file: " + path, e);
			}
		}

		try {
			return mapper.treeToValue(sourceNode, configClass);
		} catch (Exception e) {
			throw new ConfigException("Failed to bind config to " + configClass.getName(), e);
		}
	}

	@Override
	public <T> T fromBytes(byte[] bytes, Class<T> configClass) {
		if (bytes == null || bytes.length == 0) {
			try {
				return mapper.treeToValue(mapper.createObjectNode(), configClass);
			} catch (Exception e) {
				throw new ConfigException("Failed to bind empty bytes to " + configClass.getName(), e);
			}
		}

		try {
			return mapper.readValue(bytes, configClass);
		} catch (IOException e) {
			throw new ConfigException("Failed to deserialize config bytes", e);
		}
	}

	private DefaultConfigReader(Format format, AdapterRegistry registry) {
		this.format = format;
		this.registry = registry;
		this.mapper = MapperFactory.buildMapper(this.format, this.registry);
	}
}


