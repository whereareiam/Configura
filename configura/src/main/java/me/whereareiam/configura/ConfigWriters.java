package me.whereareiam.configura;

import me.whereareiam.configura.internal.ProviderResolver;
import me.whereareiam.configura.writer.ConfigWriter;
import me.whereareiam.configura.writer.ConfigWriterProvider;

public final class ConfigWriters {
	public static ConfigWriter create() {
		ConfigWriterProvider provider = ProviderResolver.loadFirst(ConfigWriterProvider.class);
		if (provider != null) return provider.create();
		throw new UnsupportedOperationException("No ConfigWriterProvider found. Add configura-common to the classpath.");
	}
}