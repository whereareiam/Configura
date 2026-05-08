package me.whereareiam.configura.common.util;

import me.whereareiam.configura.type.Format;

import java.nio.file.Path;
import java.util.Locale;

public final class FileUtil {
	private FileUtil() {
	}

	public static Path resolvePathWithFormat(String file, Format format) {
		return resolvePathWithExtension(Path.of(file), format.getExtension());
	}

	public static Path resolvePathWithFormat(Path path, Format format) {
		return resolvePathWithExtension(path, format.getExtension());
	}

	public static Path resolvePathWithExtension(String file, String extension) {
		return resolvePathWithExtension(Path.of(file), extension);
	}

	public static Path resolvePathWithExtension(Path path, String extension) {
		String file = path.toString();
		String lower = file.toLowerCase(Locale.ROOT);
		String normalizedExtension = normalizeExtension(extension);
		if (lower.endsWith(normalizedExtension) || lower.endsWith(".yml") || lower.endsWith(".yaml") || lower.endsWith(".json"))
			return path;

		return Path.of(file + normalizedExtension);
	}

	private static String normalizeExtension(String extension) {
		if (extension == null || extension.isBlank()) return "";
		return extension.startsWith(".") ? extension : "." + extension;
	}
}
