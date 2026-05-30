package me.whereareiam.configura.feature.polymorphic.module;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import me.whereareiam.configura.feature.polymorphic.DefaultPolymorphicRegistry;
import me.whereareiam.configura.feature.polymorphic.PolymorphicTypeResolver;
import me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition;

import java.io.IOException;
import java.util.Map;

public final class PolymorphicSerializationModule extends SimpleModule {
	private final DefaultPolymorphicRegistry registry;

	public PolymorphicSerializationModule(DefaultPolymorphicRegistry registry) {
		super("configura-polymorphic-feature-module", Version.unknownVersion());
		this.registry = registry;
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addBeanDeserializerModifier(new BeanDeserializerModifier() {
			@Override
			public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
				Class<?> raw = beanDesc.getBeanClass();
				PolymorphicDefinition effective = registry.definition(raw);
				if (effective == null) return deserializer;
				return new JsonDeserializer<>() {
					@Override
					public Object deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
						JsonNode node = parser.readValueAsTree();
						Class<?> target = new PolymorphicTypeResolver(registry).resolve(raw, new me.whereareiam.configura.document.DocumentTypeContext(node, null, null, null, null, null));
						ObjectMapper mapper = (ObjectMapper) parser.getCodec();
						Class<?> bindType = target != null ? target : raw;
						return mapper.treeToValue(node, bindType);
					}
				};
			}
		});
		context.addBeanSerializerModifier(new BeanSerializerModifier() {
			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
				Class<?> raw = beanDesc.getBeanClass();
				PolymorphicDefinition effective = registry.definition(raw);
				if (effective == null) return serializer;
				return new JsonSerializer<>() {
					@Override
					public void serialize(Object value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
						ObjectMapper mapper = (ObjectMapper) generator.getCodec();
						JsonNode node = mapper.valueToTree(value);
						if (effective.getDiscriminator() != null && !effective.getDiscriminator().isEmpty() && !node.has(effective.getDiscriminator())) {
							String inferred = inferDiscriminatorValue(effective, value);
							if (inferred != null && node instanceof ObjectNode objectNode)
								objectNode.put(effective.getDiscriminator(), inferred);
						}
						generator.writeTree(node);
					}
				};
			}
		});
	}

	private static String inferDiscriminatorValue(PolymorphicDefinition info, Object value) {
		for (Map.Entry<String, Class<?>> entry : info.getMappings().entrySet()) {
			if (entry.getValue().isAssignableFrom(value.getClass()))
				return entry.getKey();
		}
		return null;
	}
}
