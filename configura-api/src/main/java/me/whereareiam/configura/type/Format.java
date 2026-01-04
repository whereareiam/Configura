package me.whereareiam.configura.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Supported configuration file formats.
 */
@Getter
@RequiredArgsConstructor
public enum Format {
	/**
	 * YAML format (.yml files)
	 */
	YAML(".yml"),

	/**
	 * JSON format (.json files)
	 */
	JSON(".json");

	private final String extension;
}


