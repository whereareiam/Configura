package me.whereareiam.configura.common.duration;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.time.Duration;

public final class DurationModule extends SimpleModule {
	public DurationModule() {
		addSerializer(Duration.class, new DurationSerializer());
		addDeserializer(Duration.class, new DurationDeserializer());
	}
}
