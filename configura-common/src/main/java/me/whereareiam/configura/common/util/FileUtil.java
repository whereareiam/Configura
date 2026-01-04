package me.whereareiam.configura.common.util;

import me.whereareiam.configura.type.Format;

public final class FileUtil {
	public static String resolvePathWithFormat(String file, Format format) {
		String lower = file.toLowerCase();
		if (lower.endsWith(".yml") || lower.endsWith(".json"))
			return file;

		return file + format.getExtension();
	}
}
