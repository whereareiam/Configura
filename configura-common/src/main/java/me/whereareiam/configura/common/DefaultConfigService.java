package me.whereareiam.configura.common;

import me.whereareiam.configura.ConfigService;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.common.reader.DefaultConfigReader;
import me.whereareiam.configura.common.writer.DefaultConfigWriter;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DefaultConfigService implements ConfigService {
	private final Format defaultFormat;
	private final Map<Class<?>, Class<? extends TypeAdapter<?>>> adapters;

	public DefaultConfigService() {
		this(Format.YAML, Collections.emptyMap());
	}

	private DefaultConfigService(Format defaultFormat, Map<Class<?>, Class<? extends TypeAdapter<?>>> adapters) {
		this.defaultFormat = defaultFormat;
		this.adapters = Map.copyOf(adapters);
	}

	@Override
	public <T> T load(String filePath, Class<T> configClass) {
		ConfigReader reader = buildReader(defaultFormat);
		return reader.load(filePath, configClass);
	}

	@Override
	public <T> void save(String filePath, T config) {
		ConfigWriter writer = buildWriter(defaultFormat);
		writer.save(filePath, config);
	}

	@Override
	public <T> T updateRead(String filePath, T config) {
		ConfigWriter writer = buildWriter(defaultFormat);
		return writer.updateRead(filePath, config);
	}

	@Override
	public boolean exists(String filePath) {
		ConfigReader reader = buildReader(defaultFormat);
		return reader.exists(filePath);
	}

	@Override
	public Format getDefaultFormat() {
		return defaultFormat;
	}

	@Override
	public Map<Class<?>, Class<? extends TypeAdapter<?>>> getRegisteredAdapters() {
		return adapters;
	}

	@Override
	public ConfigService withDefaultFormat(Format format) {
		return new DefaultConfigService(format, adapters);
	}

	@Override
	public <T> ConfigService withAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		Map<Class<?>, Class<? extends TypeAdapter<?>>> next = new HashMap<>(adapters);
		next.put(type, adapterClass);

		return new DefaultConfigService(defaultFormat, next);
	}

	private ConfigReader buildReader(Format format) {
		ConfigReader reader = new DefaultConfigReader().withFormat(format);
		for (Map.Entry<Class<?>, Class<? extends TypeAdapter<?>>> e : adapters.entrySet()) {
			@SuppressWarnings("unchecked")
			Class<Object> key = (Class<Object>) e.getKey();
			@SuppressWarnings("unchecked")
			Class<? extends TypeAdapter<Object>> value = (Class<? extends TypeAdapter<Object>>) e.getValue();
			reader = reader.registerAdapter(key, value);
		}

		return reader;
	}

	private ConfigWriter buildWriter(Format format) {
		ConfigWriter writer = new DefaultConfigWriter().withFormat(format);
		for (Map.Entry<Class<?>, Class<? extends TypeAdapter<?>>> e : adapters.entrySet()) {
			@SuppressWarnings("unchecked")
			Class<Object> key = (Class<Object>) e.getKey();
			@SuppressWarnings("unchecked")
			Class<? extends TypeAdapter<Object>> value = (Class<? extends TypeAdapter<Object>>) e.getValue();
			writer = writer.registerAdapter(key, value);
		}

		return writer;
	}
}
