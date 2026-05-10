package me.whereareiam.configura.common.duration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import java.io.IOException;
import java.time.Duration;

public final class DurationDeserializer extends StdScalarDeserializer<Duration> {
	public DurationDeserializer() {
		super(Duration.class);
	}

	@Override
	public Duration deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		return DurationFormat.parse(parser.getValueAsString());
	}
}
