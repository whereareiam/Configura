package me.whereareiam.configura.common.merge.defaults.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.common.merge.defaults.FieldDefaultsResolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceDefaultsResolver implements FieldDefaultsResolver {
	@Override
	public JsonNode resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Defaults defaults = field.getAnnotation(Defaults.class);
		if (defaults == null || defaults.source().value().isEmpty()) return null;
		return loadResource(mapper, defaults.source().value());
	}

	private static JsonNode loadResource(ObjectMapper mapper, String ref) {
		if (ref.startsWith("classpath:")) {
			String path = ref.substring("classpath:".length());
			try (var in = SourceDefaultsResolver.class.getResourceAsStream(path)) {
				if (in == null) return null;
				try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
					return mapper.readTree(reader);
				}
			} catch (Exception ignored) {
				return null;
			}
		}

		if (ref.startsWith("file:")) {
			Path path = Path.of(ref.substring("file:".length()));
			try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				return mapper.readTree(reader);
			} catch (Exception ignored) {
				return null;
			}
		}

		return null;
	}
}
