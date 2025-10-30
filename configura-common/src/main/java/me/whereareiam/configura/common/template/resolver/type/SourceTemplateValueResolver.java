package me.whereareiam.configura.common.template.resolver.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.template.resolver.TemplateValueResolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceTemplateValueResolver implements TemplateValueResolver {
	@Override
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Template t = field.getAnnotation(Template.class);
		if (t == null || t.source() == null || t.source().value().isEmpty()) return null;
		return loadResource(mapper, t.source().value());
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


