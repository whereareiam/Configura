package me.whereareiam.configura;

import me.whereareiam.configura.internal.ProviderResolver;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.util.Objects;

@SuppressWarnings("unused")
public final class Config {
	private static ConfigReader defaultReader;
	private static ConfigWriter defaultWriter;
	private static volatile ConfigService service;

	public static void setDefaultReader(ConfigReader reader) {
		Config.defaultReader = reader;
	}

	public static void setDefaultWriter(ConfigWriter writer) {
		Config.defaultWriter = writer;
	}

	public static void init(ConfigService svc) {
		service = Objects.requireNonNull(svc);
	}

	public static <T> void registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		ConfigService s = getServiceOrNull();
		if (s != null) {
			service = s.withAdapter(type, adapterClass);
			return;
		}

		getDefaultReader().registerAdapter(type, adapterClass);
		getDefaultWriter().registerAdapter(type, adapterClass);
	}

	public static <T> T load(String filePath, Class<T> configClass) {
		ConfigService s = getServiceOrNull();
		if (s != null) return s.load(filePath, configClass);

		return getReaderForFile(filePath).load(filePath, configClass);
	}

	public static <T> void save(String filePath, T config) {
		ConfigService s = getServiceOrNull();
		if (s != null) {
			s.save(filePath, config);
			return;
		}

		getWriterForFile(filePath).save(filePath, config);
	}

	public static <T> T updateRead(String filePath, T config) {
		ConfigService s = getServiceOrNull();
		if (s != null) return s.updateRead(filePath, config);

		return getWriterForFile(filePath).updateRead(filePath, config);
	}

	public static ConfigReader getDefaultReader() {
		if (defaultReader == null) defaultReader = ConfigReaders.create().withFormat(Format.YAML);
		return defaultReader;
	}

	private static ConfigWriter getDefaultWriter() {
		if (defaultWriter == null) defaultWriter = ConfigWriters.create().withFormat(Format.YAML);
		return defaultWriter;
	}

	private static ConfigService getServiceOrNull() {
		ConfigService s = service;
		if (s != null) return s;

		ConfigService impl = ProviderResolver.loadFirst(ConfigService.class);
		if (impl != null) {
			service = impl;
			return impl;
		}

		return null;
	}

	private static ConfigReader getReaderForFile(String filePath) {
		Format detectedFormat = detectFormat(filePath);
		if (defaultReader != null && defaultReader.getFormat() == detectedFormat)
			return defaultReader;

		return ConfigReaders.create().withFormat(detectedFormat);
	}

	private static ConfigWriter getWriterForFile(String filePath) {
		Format detectedFormat = detectFormat(filePath);
		if (defaultWriter != null && defaultWriter.getFormat() == detectedFormat)
			return defaultWriter;

		return ConfigWriters.create().withFormat(detectedFormat);
	}

	private static Format detectFormat(String filePath) {
		String lower = filePath.toLowerCase();
		if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return Format.YAML;
		if (lower.endsWith(".json")) return Format.JSON;
		return Format.YAML;
	}
}