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
		if (value == null || value.asList().isEmpty()) {
			gen.writeStartArray();
			gen.writeEndArray();
			return;
		}

		var list = value.asList();
		if (value.isSinglePreferred() && list.size() == 1) {
			serializers.defaultSerializeValue(list.get(0), gen);
			return;
		}

		gen.writeStartArray();
		for (Object element : list) {
			serializers.defaultSerializeValue(element, gen);
		}
		gen.writeEndArray();
	}
}
