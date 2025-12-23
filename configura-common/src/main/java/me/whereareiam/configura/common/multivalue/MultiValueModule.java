package me.whereareiam.configura.common.multivalue;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.configura.type.MultiValue;

/**
 * Registers serializers/deserializers for {@link MultiValue}.
 */
public class MultiValueModule extends SimpleModule {
	public MultiValueModule() {
		super("configura-multivalue-module", Version.unknownVersion());
		addSerializer(MultiValue.class, new MultiValueSerializer());
		addDeserializer(MultiValue.class, new MultiValueDeserializer());
	}
}
