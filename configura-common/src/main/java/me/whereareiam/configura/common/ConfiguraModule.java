package me.whereareiam.configura.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.common.serialization.FieldDefaultsModifier;

import java.io.IOException;
import java.util.Map;

/**
 * Composer that registers adapters and focused modifiers (kept small).
 */
public final class ConfiguraModule extends SimpleModule {
	private final Map<Class<?>, TypeAdapter<?>> adapters;

	@SuppressWarnings("unchecked")
	public ConfiguraModule(Map<Class<?>, TypeAdapter<?>> adapters) {
		super("configura-module", Version.unknownVersion());
		this.adapters = adapters;
		if (adapters == null || adapters.isEmpty()) return;
		for (Map.Entry<Class<?>, TypeAdapter<?>> entry : adapters.entrySet()) {
			Class<Object> targetType = (Class<Object>) entry.getKey();
			TypeAdapter<Object> adapter = (TypeAdapter<Object>) entry.getValue();
			registerAdapterHandlers(targetType, adapter);
		}
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addBeanDeserializerModifier(new FieldDefaultsModifier(this.adapters));
	}

	private void registerAdapterHandlers(Class<Object> type, TypeAdapter<Object> adapter) {
		addSerializer(type, new JsonSerializer<>() {
			@Override
			public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
				try {
					gen.writeString(adapter.serialize(value));
				} catch (Exception ex) {
					throw new JsonMappingException(gen, "Adapter serialization failed for " + type.getName(), ex);
				}
			}
		});

		addDeserializer(type, new JsonDeserializer<>() {
			@Override
			public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
				String raw = p.currentToken() != null && p.currentToken().isScalarValue()
						? p.getValueAsString()
						: asText(p.readValueAsTree());

				try {
					return adapter.deserialize(stripQuotes(raw));
				} catch (Exception ex) {
					throw new JsonMappingException(p, "Adapter deserialization failed for " + type.getName(), ex);
				}
			}
		});
	}

	private static String asText(JsonNode node) {
		return node == null ? null : node.asText();
	}

	private static String stripQuotes(String text) {
		if (text == null) return null;
		String t = text.trim();
		if (t.length() >= 2 && ((t.startsWith("\"") && t.endsWith("\""))
				|| (t.startsWith("'") && t.endsWith("'"))))
			return t.substring(1, t.length() - 1);

		return t;
	}
}



