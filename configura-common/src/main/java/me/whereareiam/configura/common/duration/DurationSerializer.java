package me.whereareiam.configura.common.duration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.time.Duration;

public final class DurationSerializer extends StdScalarSerializer<Duration> {
	public DurationSerializer() {
		super(Duration.class);
	}

	@Override
	public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		gen.writeString(DurationFormat.format(value));
	}
}
