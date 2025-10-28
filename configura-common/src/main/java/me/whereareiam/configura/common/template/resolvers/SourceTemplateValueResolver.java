package me.whereareiam.configura.common.template.resolvers;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.template.Source;
import me.whereareiam.configura.common.template.TemplateValueResolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceTemplateValueResolver implements TemplateValueResolver {
	@Override
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Source src = field.getAnnotation(Source.class);
		if (src == null || src.value().isEmpty()) return null;

		return loadResource(mapper, src.value());
	}

	private static Object loadResource(ObjectMapper mapper, String ref) {
		if (ref.startsWith("classpath:")) {
			String path = ref.substring("classpath:".length());
			try (var in = SourceTemplateValueResolver.class.getResourceAsStream(path)) {
				if (in == null) return null;
				try (var r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
					return mapper.readTree(r);
				}
			} catch (Exception ignored) {
				return null;
			}
		}

		if (ref.startsWith("file:")) {
			Path p = Path.of(ref.substring("file:".length()));
			try (var r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				return mapper.readTree(r);
			} catch (Exception ignored) {
				return null;
			}
		}

		return null;
	}
}


