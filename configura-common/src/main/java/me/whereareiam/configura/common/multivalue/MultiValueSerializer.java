package me.whereareiam.configura.common.multivalue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import me.whereareiam.configura.type.MultiValue;

import java.io.IOException;

/**
 * Serializes {@link MultiValue} as a JSON/YAML array.
 */
public class MultiValueSerializer extends JsonSerializer<MultiValue> {
	@Override
	public void serialize(MultiValue value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeStartArray();
		if (value != null) {
			for (Object element : value.asList()) {
				serializers.defaultSerializeValue(element, gen);
			}
		}
		gen.writeEndArray();
	}
}
