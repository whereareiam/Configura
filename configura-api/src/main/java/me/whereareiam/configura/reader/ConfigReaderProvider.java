package me.whereareiam.configura.reader;

/**
 * Internal SPI for DI/ServiceLoader integration. Not intended for direct use.
 */
public interface ConfigReaderProvider {
    ConfigReader create();
}


