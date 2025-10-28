package me.whereareiam.configura.exception;

/**
 * Exception thrown when configuration operations fail.
 */
@SuppressWarnings("unused")
public class ConfigException extends RuntimeException {
	public ConfigException(String message) {
		super(message);
	}

	public ConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}


