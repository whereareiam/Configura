package me.whereareiam.configura.common.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.adapter.AdapterRegistry;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.template.TemplateRegistry;
import me.whereareiam.configura.type.Format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultConfigReader implements ConfigReader {
	private final Format format;
	private final AdapterRegistry registry;
	private final TemplateRegistry templateRegistry;
	private final ObjectMapper mapper;

	public DefaultConfigReader() {
		this(null);
	}

	public DefaultConfigReader(TemplateRegistry templateRegistry) {
		this.format = Format.YAML;
		this.registry = AdapterRegistry.empty();
		this.templateRegistry = templateRegistry;
		this.mapper = MapperFactory.buildReaderMapper(this.format, this.registry);
	}

	private DefaultConfigReader(
			Format format,
			AdapterRegistry registry,
			TemplateRegistry templateRegistry
	) {
		this.format = format;
		this.registry = registry;
		this.templateRegistry = templateRegistry;
		this.mapper = MapperFactory.buildReaderMapper(this.format, this.registry);
	}

	@Override
	public ConfigReader withFormat(Format format) {
		return new DefaultConfigReader(format, this.registry, this.templateRegistry);
	}

	@Override
	public <T> ConfigReader registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		AdapterRegistry nextRegistry = this.registry.withAdapter(type, adapterClass);
		return new DefaultConfigReader(this.format, nextRegistry, this.templateRegistry);
	}

	@Override
	public Format getFormat() {
		return format;
	}

	@Override
	public <T> T decode(byte[] bytes, Class<T> configClass) {
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

	@Override
	public <T> T decode(InputStream inputStream, Class<T> configClass) {
		if (configClass == null) throw new ConfigException("configClass must not be null");
		if (inputStream == null) {
			try {
				return mapper.treeToValue(mapper.createObjectNode(), configClass);
			} catch (Exception e) {
				throw new ConfigException("Failed to bind empty stream to " + configClass.getName(), e);
			}
		}

		try {
			return mapper.readValue(inputStream, configClass);
		} catch (IOException e) {
			throw new ConfigException("Failed to deserialize config stream", e);
		}
	}

	@Override
	public <T> T read(String fileName, Class<T> configClass) {
		if (fileName == null) throw new ConfigException("fileName must not be null");
		String resolved = FileUtil.resolvePathWithFormat(fileName, format);
		return read(Path.of(resolved), configClass, mapper);
	}

	@Override
	public <T> T read(Path path, Class<T> configClass) {
		if (path == null) throw new ConfigException("path must not be null");
		if (configClass == null) throw new ConfigException("configClass must not be null");

		String resolved = FileUtil.resolvePathWithFormat(path.toString(), format);
		Path target = Path.of(resolved);

		if (!Files.exists(target)) throw new ConfigException("Config file does not exist: " + target);

		try (BufferedReader reader = Files.newBufferedReader(target)) {
			JsonNode node = mapper.readTree(reader);
			if (node == null) throw new ConfigException("Config file is empty or invalid: " + target);

			return mapper.treeToValue(node, configClass);
		} catch (IOException e) {
			throw new ConfigException("Failed to read config file: " + target, e);
		} catch (Exception e) {
			throw new ConfigException("Failed to bind config to " + configClass.getName(), e);
		}
	}

	private <T> T read(Path path, Class<T> configClass, ObjectMapper om) {
		if (!Files.exists(path)) throw new ConfigException("Config file does not exist: " + path);
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			JsonNode node = om.readTree(reader);
			if (node == null) throw new ConfigException("Config file is empty or invalid: " + path);
			return om.treeToValue(node, configClass);
		} catch (IOException e) {
			throw new ConfigException("Failed to read config file: " + path, e);
		} catch (Exception e) {
			throw new ConfigException("Failed to bind config to " + configClass.getName(), e);
		}
	}

	public <T> T load(String fileName, Class<T> configClass) {
		if (fileName == null) throw new ConfigException("fileName must not be null");
		if (configClass == null) throw new ConfigException("configClass must not be null");

		String resolved = FileUtil.resolvePathWithFormat(fileName, format);

		return read(Path.of(resolved), configClass);
	}

	public <T> T load(Path path, Class<T> configClass) {
		return read(path, configClass);
	}

	@Override
	public <T> T load(byte[] bytes, Class<T> configClass) {
		return decode(bytes, configClass);
	}

	@Override
	public <T> T load(InputStream inputStream, Class<T> configClass) {
		return decode(inputStream, configClass);
	}

	public ConfigReader withTemplateRegistry(TemplateRegistry templateRegistry) {
		return new DefaultConfigReader(this.format, this.registry, templateRegistry);
	}
}


