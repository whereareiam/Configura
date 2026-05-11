package me.whereareiam.configura.common.merge.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.common.merge.defaults.type.*;

import java.lang.reflect.Field;
import java.util.List;

public interface FieldDefaultsResolver {
	JsonNode resolve(ObjectMapper mapper, Class<?> targetType, Field field);

	static List<FieldDefaultsResolver> standard(DefaultMergeDefaultsRegistry registry) {
		return List.of(
				new FieldProviderDefaultsResolver(),
				new SourceDefaultsResolver(),
				new InlineLiteralDefaultsResolver(),
				new InlineListDefaultsResolver(),
				new InlineObjectDefaultsResolver(),
				new ModelDefaultsResolver(registry)
		);
	}
}
