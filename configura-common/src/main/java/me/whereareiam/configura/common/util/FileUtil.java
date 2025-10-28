package me.whereareiam.configura.common.util;

import me.whereareiam.configura.type.Format;

public final class FileUtil {
	public static String resolvePathWithFormat(String filePath, Format format) {
		String lower = filePath.toLowerCase();
		if (lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".json"))
			return filePath;

		return switch (format) {
			case YAML -> filePath + ".yaml";
			case JSON -> filePath + ".json";
		};
	}
}


