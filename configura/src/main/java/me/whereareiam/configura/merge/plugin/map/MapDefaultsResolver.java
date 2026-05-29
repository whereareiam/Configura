package me.whereareiam.configura.merge.plugin.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.merge.plugin.property.PropertyDefaultsResolver;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public final class MapDefaultsResolver {
	private final PropertyDefaultsResolver propertyDefaultsResolver = new PropertyDefaultsResolver();

	public @Nullable JsonNode resolve(ObjectMapper mapper, Field property) {
		JsonNode inline = propertyDefaultsResolver.resolveInlineObject(mapper, property);
		if (inline != null) return inline;

		Defaults defaults = property.getAnnotation(Defaults.class);
		if (defaults == null) return null;
		if (!defaults.source().value().isEmpty())
			return propertyDefaultsResolver.resolveSource(mapper, property);
		if (defaults.provider().value() != Defaults.Provider.None.class)
			return propertyDefaultsResolver.resolveProvider(mapper, property.getType(), property);

		return null;
	}
}
