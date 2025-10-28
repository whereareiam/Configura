package me.whereareiam.configura.common.writer;

import me.whereareiam.configura.writer.ConfigWriter;
import me.whereareiam.configura.writer.ConfigWriterProvider;

public class DefaultConfigWriterProvider implements ConfigWriterProvider {
    @Override
    public ConfigWriter create() {
        return new DefaultConfigWriter();
    }
}


