package me.whereareiam.configura;

import me.whereareiam.configura.internal.ProviderResolver;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.reader.ConfigReaderProvider;

public final class ConfigReaders {
	public static ConfigReader create() {
		ConfigReaderProvider provider = ProviderResolver.loadFirst(ConfigReaderProvider.class);
		if (provider != null) return provider.create();
		throw new UnsupportedOperationException("No ConfigReaderProvider found. Add configura-common to the classpath.");
	}
}


