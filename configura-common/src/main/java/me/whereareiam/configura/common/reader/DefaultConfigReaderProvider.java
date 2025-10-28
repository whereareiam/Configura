package me.whereareiam.configura.common.reader;

import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.reader.ConfigReaderProvider;

public class DefaultConfigReaderProvider implements ConfigReaderProvider {
    @Override
    public ConfigReader create() {
        return new DefaultConfigReader();
    }
}


