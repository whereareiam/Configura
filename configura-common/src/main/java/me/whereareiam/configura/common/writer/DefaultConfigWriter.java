package me.whereareiam.configura.common.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.annotation.Policy;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.adapter.AdapterRegistry;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultConfigWriter implements ConfigWriter {
	private final Format format;
	private final AdapterRegistry registry;
	private final ObjectMapper mapper;

	public DefaultConfigWriter() {
		this.format = Format.YAML;
		this.registry = AdapterRegistry.empty();
		this.mapper = MapperFactory.buildMapper(this.format, this.registry);
	}

	@Override
	public <T> byte[] save(T config) {
		try {
			return mapper.writeValueAsBytes(config);
		} catch (IOException e) {
			throw new ConfigException("Failed to serialize config to bytes", e);
		}
	}

	@Override
	public ConfigWriter withFormat(Format format) {
		return new DefaultConfigWriter(format, this.registry);
	}

	@Override
	public <T> ConfigWriter registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		AdapterRegistry next = this.registry.withAdapter(type, adapterClass);
		return new DefaultConfigWriter(this.format, next);
	}

	@Override
	public Format getFormat() {
		return format;
	}

	@Override
	public <T> void save(String file, T config) {
		String resolved = FileUtil.resolvePathWithFormat(file, format);
		try {
			Path path = Path.of(resolved);
			Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));

			T toWrite = mergePreservingPolicy(path, config);

			mapper.writeValue(path.toFile(), toWrite);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + resolved, e);
		}
	}

	@Override
	public <T> void save(Path path, T config) {
		String resolved = FileUtil.resolvePathWithFormat(path.toString(), format);
		try {
			Path target = Path.of(resolved);
			Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));

			T toWrite = mergePreservingPolicy(target, config);

			mapper.writeValue(target.toFile(), toWrite);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + resolved, e);
		}
	}

	@Override
	public <T> T merge(String file, T config) {
		String resolved = FileUtil.resolvePathWithFormat(file, format);
		Path path = Path.of(resolved);

		if (Files.exists(path)) return mergePreservingPolicy(path, config);

		return config;
	}

	@Override
	public <T> T merge(Path path, T config) {
		String resolved = FileUtil.resolvePathWithFormat(path.toString(), format);
		Path target = Path.of(resolved);

		if (Files.exists(target)) return mergePreservingPolicy(target, config);

		return config;
	}

	@SuppressWarnings("unchecked")
	private <T> T mergePreservingPolicy(Path path, T incoming) {
		Class<?> configClass = incoming.getClass();
		try {
			Object existing = mapper.readValue(path.toFile(), (Class<Object>) configClass);
			if (existing == null) {
				return incoming;
			}

			for (Field field : configClass.getDeclaredFields()) {
				Policy policy = field.getAnnotation(Policy.class);
				if (policy != null && !policy.mergeOnUpdate()) {
					boolean accessible = field.canAccess(incoming);
					field.setAccessible(true);
					Object existingValue = field.get(existing);
					if (existingValue != null) field.set(incoming, existingValue);

					field.setAccessible(accessible);
				}
			}
			return incoming;
		} catch (Exception e) {
			// If anything goes wrong, fall back to writing the incoming model
			return incoming;
		}
	}

	private DefaultConfigWriter(Format format, AdapterRegistry registry) {
		this.format = format;
		this.registry = registry;
		this.mapper = MapperFactory.buildMapper(this.format, this.registry);
	}
}