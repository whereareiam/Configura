package me.whereareiam.configura.merge.plugin.list;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.merge.plugin.property.PropertyDefaultsResolver;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public final class ListDefaultsResolver {
	private final PropertyDefaultsResolver propertyDefaultsResolver = new PropertyDefaultsResolver();

	public @Nullable JsonNode resolve(ObjectMapper mapper, Field property) {
		Defaults defaults = property.getAnnotation(Defaults.class);
		if (defaults == null) return null;

		if (defaults.items().length > 0) return buildArray(mapper, defaults.items());
		if (defaults.stringItems().length > 0) {
			ArrayNode array = mapper.createArrayNode();
			for (String value : defaults.stringItems())
				array.add(value);

			return array;
		}
		if (defaults.list().items().length > 0)
			return buildArray(mapper, defaults.list().items());

		if (!defaults.source().value().isEmpty())
			return propertyDefaultsResolver.resolveSource(mapper, property);
		if (defaults.provider().value() != Defaults.Provider.None.class)
			return propertyDefaultsResolver.resolveProvider(mapper, property.getType(), property);

		return null;
	}

	private static ArrayNode buildArray(ObjectMapper mapper, Defaults.Literal[] literals) {
		ArrayNode array = mapper.createArrayNode();
		for (Defaults.Literal literal : literals) {
			Object value = PropertyDefaultsResolver.parseLiteral(literal);
			if (value != null) array.add(mapper.valueToTree(value));
		}

		return array;
	}
}
