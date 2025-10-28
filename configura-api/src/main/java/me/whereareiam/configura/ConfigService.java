package me.whereareiam.configura;

import me.whereareiam.configura.type.Format;

import java.util.Map;

/**
 * Immutable configuration service used by the static {@link Config} facade.
 */
@SuppressWarnings("unused")
public interface ConfigService {
	<T> T load(String filePath, Class<T> configClass);

	<T> void save(String filePath, T config);

	<T> T updateRead(String filePath, T config);

	boolean exists(String filePath);

	Format getDefaultFormat();

	Map<Class<?>, Class<? extends TypeAdapter<?>>> getRegisteredAdapters();

	ConfigService withDefaultFormat(Format format);

	<T> ConfigService withAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass);
}
