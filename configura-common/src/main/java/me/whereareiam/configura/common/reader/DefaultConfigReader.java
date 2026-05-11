package me.whereareiam.configura.common.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.processor.PostProcessor;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultConfigReader implements ConfigReader {
	private final Format format;
	private final ObjectMapper mapper;

	public DefaultConfigReader() {
		this.format = Format.YAML;
		this.mapper = MapperFactory.buildReaderMapper(this.format);
	}

	private DefaultConfigReader(Format format) {
		this.format = format;
		this.mapper = MapperFactory.buildReaderMapper(this.format);
	}

	@Override
	public ConfigReader withFormat(Format format) {
		return new DefaultConfigReader(format);
	}

	@Override
	public Format getFormat() {
		return format;
	}

	@Override
	public <T> T decode(byte[] bytes, Class<T> configClass) {
		T config;
		if (bytes == null || bytes.length == 0) {
			try {
				config = mapper.treeToValue(mapper.createObjectNode(), configClass);
			} catch (Exception e) {
				throw new ConfigException("Failed to bind empty bytes to " + configClass.getName(), e);
			}
		} else {
			try {
				config = mapper.readValue(bytes, configClass);
			} catch (IOException e) {
				throw new ConfigException("Failed to deserialize config bytes", e);
			}
		}

		PostProcessor.process(config);
		return config;
	}

	@Override
	public <T> T decode(InputStream inputStream, Class<T> configClass) {
		if (configClass == null) throw new ConfigException("configClass must not be null");
		T config;
		if (inputStream == null) {
			try {
				config = mapper.treeToValue(mapper.createObjectNode(), configClass);
			} catch (Exception e) {
				throw new ConfigException("Failed to bind empty stream to " + configClass.getName(), e);
			}
		} else {
			try {
				config = mapper.readValue(inputStream, configClass);
			} catch (IOException e) {
				throw new ConfigException("Failed to deserialize config stream", e);
			}
		}

		PostProcessor.process(config);
		return config;
	}

	@Override
	public <T> T read(String fileName, Class<T> configClass) {
		if (fileName == null) throw new ConfigException("fileName must not be null");
		return read(FileUtil.resolvePathWithFormat(fileName, format), configClass, mapper);
	}

	@Override
	public <T> T read(Path path, Class<T> configClass) {
		if (path == null) throw new ConfigException("path must not be null");
		if (configClass == null) throw new ConfigException("configClass must not be null");

		Path target = FileUtil.resolvePathWithFormat(path, format);

		if (!Files.exists(target)) throw new ConfigException("Config file does not exist: " + target);

		try (BufferedReader reader = Files.newBufferedReader(target)) {
			JsonNode node = mapper.readTree(reader);
			if (node == null) throw new ConfigException("Config file is empty or invalid: " + target);

			T config = mapper.treeToValue(node, configClass);
			PostProcessor.process(config);

			return config;
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

			T config = om.treeToValue(node, configClass);
			PostProcessor.process(config);

			return config;
		} catch (IOException e) {
			throw new ConfigException("Failed to read config file: " + path, e);
		} catch (Exception e) {
			throw new ConfigException("Failed to bind config to " + configClass.getName(), e);
		}
	}

	public <T> T load(String fileName, Class<T> configClass) {
		if (fileName == null) throw new ConfigException("fileName must not be null");
		if (configClass == null) throw new ConfigException("configClass must not be null");

		return read(FileUtil.resolvePathWithFormat(fileName, format), configClass);
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

}
