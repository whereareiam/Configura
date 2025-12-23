package me.whereareiam.configura.common.multivalue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import me.whereareiam.configura.type.MultiValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializes a scalar or array into a {@link MultiValue}, preserving the element type when available.
 */
public class MultiValueDeserializer extends JsonDeserializer<MultiValue> implements ContextualDeserializer {
	private final JavaType contentType;

	public MultiValueDeserializer() {
		this(null);
	}

	private MultiValueDeserializer(JavaType contentType) {
		this.contentType = contentType;
	}

	@Override
	public MultiValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		ObjectCodec codec = p.getCodec();
		JsonNode node = codec.readTree(p);

		JavaType ct = contentType != null ? contentType : ctxt.getTypeFactory().constructType(Object.class);
		ObjectMapper mapper = (ObjectMapper) codec;

		List<Object> values = new ArrayList<>();
		if (node == null || node.isNull()) {
			return new MultiValue<>(values);
		}

		if (node.isArray()) {
			for (JsonNode element : node) {
				values.add(mapper.convertValue(element, ct));
			}
			return new MultiValue<>(values);
		}

		values.add(mapper.convertValue(node, ct));
		return new MultiValue<>(values);
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		JavaType targetType = property != null ? property.getType() : ctxt.getContextualType();
		JavaType content = (targetType != null && targetType.containedTypeCount() > 0)
				? targetType.containedType(0)
				: ctxt.getTypeFactory().constructType(Object.class);

		return new MultiValueDeserializer(content);
	}
}
