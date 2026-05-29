package me.whereareiam.configura.common.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public final class DefaultConfigReader implements ConfigReader {
	private final String extension;
	private final ObjectMapper mapper;

	public DefaultConfigReader() {
		this(Format.YAML);
	}

	public DefaultConfigReader(Format format) {
		this(format.getExtension(), MapperFactory.buildReaderMapper(format));
	}

	@Override
	public <T> T read(Path path, Class<T> configClass) {
		if (path == null) throw new ConfigException("path must not be null");
		if (configClass == null) throw new ConfigException("configClass must not be null");

		T config = bind(readNode(path), configClass, "Failed to bind config to " + configClass.getName());
		PostProcessor.process(config);
		return config;
	}

	@Override
	public <T> T read(byte[] bytes, Class<T> configClass) {
		if (configClass == null) throw new ConfigException("configClass must not be null");
		T config = bind(readNode(bytes), configClass, "Failed to read config bytes for " + configClass.getName());
		PostProcessor.process(config);
		return config;
	}

	@Override
	public <T> T read(InputStream inputStream, Class<T> configClass) {
		if (configClass == null) throw new ConfigException("configClass must not be null");
		T config = bind(readNode(inputStream), configClass, "Failed to read config stream for " + configClass.getName());
		PostProcessor.process(config);
		return config;
	}

	@Override
	public JsonNode readNode(Path path) {
		if (path == null) throw new ConfigException("path must not be null");
		Path target = resolve(path);
		if (!Files.exists(target)) throw new ConfigException("Config file does not exist: " + target);

		try (BufferedReader reader = Files.newBufferedReader(target)) {
			JsonNode node = mapper.readTree(reader);
			if (node == null) throw new ConfigException("Config file is empty or invalid: " + target);
			return node;
		} catch (IOException e) {
			throw new ConfigException("Failed to read config file: " + target, e);
		}
	}

	@Override
	public JsonNode readNode(byte[] bytes) {
		if (bytes == null || bytes.length == 0)
			return mapper.createObjectNode();

		try {
			JsonNode node = mapper.readTree(bytes);
			return node != null ? node : mapper.createObjectNode();
		} catch (IOException e) {
			throw new ConfigException("Failed to read config tree from bytes", e);
		}
	}

	@Override
	public JsonNode readNode(InputStream inputStream) {
		if (inputStream == null)
			return mapper.createObjectNode();

		try {
			JsonNode node = mapper.readTree(inputStream);
			return node != null ? node : mapper.createObjectNode();
		} catch (IOException e) {
			throw new ConfigException("Failed to read config tree from stream", e);
		}
	}

	private Path resolve(String file) {
		return FileUtil.resolvePathWithExtension(file, extension);
	}

	private Path resolve(Path path) {
		return FileUtil.resolvePathWithExtension(path, extension);
	}

	private <T> T bind(JsonNode node, Class<T> configClass, String failureMessage) {
		try {
			return mapper.treeToValue(node != null ? node : mapper.createObjectNode(), configClass);
		} catch (Exception e) {
			throw new ConfigException(failureMessage, e);
		}
	}
}
