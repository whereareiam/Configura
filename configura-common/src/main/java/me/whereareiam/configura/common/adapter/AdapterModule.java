package me.whereareiam.configura.common.adapter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.configura.TypeAdapter;

import java.io.IOException;
import java.util.Map;

/**
 * Jackson module that wires Configura TypeAdapter serializers/deserializers.
 */
public final class AdapterModule extends SimpleModule {
	public AdapterModule() {
		this(null);
	}

	@SuppressWarnings("unchecked")
	public AdapterModule(Map<Class<?>, TypeAdapter<?>> adapters) {
		super("configura-adapter-module", Version.unknownVersion());
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

		// Register key serializer so map keys of this type are serialized using the adapter
		addKeySerializer(type, new JsonSerializer<>() {
			@Override
			public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
				try {
					// Write the key as a field name using the adapter's serialized form
					gen.writeFieldName(adapter.serialize(value));
				} catch (Exception ex) {
					throw new JsonMappingException(gen, "Adapter key serialization failed for " + type.getName(), ex);
				}
			}
		});

		// Register key deserializer so map keys are deserialized using the adapter
		addKeyDeserializer(type, new KeyDeserializer() {
			@Override
			public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
				try {
					// Adapter expects raw string; pass it through directly
					return adapter.deserialize(key);
				} catch (Exception ex) {
					throw new IOException("Adapter key deserialization failed for " + type.getName(), ex);
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