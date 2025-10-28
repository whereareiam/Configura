package me.whereareiam.configura.writer;

/**
 * Internal SPI for DI/ServiceLoader integration. Not intended for direct use.
 */
public interface ConfigWriterProvider {
    ConfigWriter create();
}


