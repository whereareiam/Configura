package me.whereareiam.configura.common.polymorphic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import me.whereareiam.configura.annotation.Polymorphic;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jackson module that provides polymorphic read/write using discriminator mapping.
 */
public final class PolymorphicModule extends SimpleModule {
	public PolymorphicModule() {
		super("configura-polymorphic-module", Version.unknownVersion());
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);

		context.addBeanDeserializerModifier(new BeanDeserializerModifier() {
			@Override
			public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
				Class<?> raw = beanDesc.getBeanClass();
				PolymorphicDefinition effective = effectiveInfoFor(raw);
				if (effective == null) return deserializer;
				return new JsonDeserializer<>() {
					@Override
					public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
						JsonNode node = p.readValueAsTree();
						Class<?> target = resolveTarget(node, raw, effective);
						ObjectCodec codec = p.getCodec();
						return codec.treeToValue(node, target);
					}
				};
			}
		});

		context.addBeanSerializerModifier(new BeanSerializerModifier() {
			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
				Class<?> raw = beanDesc.getBeanClass();
				PolymorphicDefinition effective = effectiveInfoFor(raw);
				if (effective == null) return serializer;

				return new JsonSerializer<>() {
					@Override
					public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
						ObjectMapper mapper = (ObjectMapper) gen.getCodec();
						JsonNode node = mapper.valueToTree(value);
						if (effective.getDiscriminator() != null && !effective.getDiscriminator().isEmpty() && !node.has(effective.getDiscriminator())) {
							String inferred = inferDiscriminatorValue(effective, value);
							if (inferred != null && node instanceof ObjectNode on)
								on.put(effective.getDiscriminator(), inferred);
						}
						gen.writeTree(node);
					}
				};
			}
		});
	}

	private static PolymorphicDefinition effectiveInfoFor(Class<?> raw) {
		PolymorphicDefinition info = PolymorphicRegistry.get(raw);

		if (info != null) return info;
		Polymorphic anno = raw.getAnnotation(Polymorphic.class);

		return fromAnnotation(anno);
	}

	private static Class<?> resolveTarget(JsonNode node, Class<?> raw, PolymorphicDefinition info) {
		Class<?> target = null;

		// 1) discriminator path (if configured and present)
		if (info.getDiscriminator() != null && !info.getDiscriminator().isEmpty()) {
			JsonNode dv = node.get(info.getDiscriminator());
			String key = dv != null && dv.isTextual() ? dv.asText() : info.getDefaultValue();

			if (key != null && !key.isEmpty())
				target = info.getMappings().get(key);
		}

		// 2) inference path (first matching field wins)
		if (target == null && info.getInferFields() != null) {
			for (Map.Entry<String, Class<?>> e : info.getInferFields().entrySet()) {
				if (node.has(e.getKey())) {
					target = e.getValue();
					break;
				}
			}
		}

		// 3) default target if provided
		if (target == null && info.getDefaultTarget() != null
				&& info.getDefaultTarget() != Void.class)
			target = info.getDefaultTarget();

		return target != null ? target : raw;
	}

	private static String inferDiscriminatorValue(PolymorphicDefinition info, Object value) {
		return info.getMappings().entrySet().stream()
				.filter(e -> e.getValue().isAssignableFrom(value.getClass()))
				.map(Map.Entry::getKey)
				.findFirst().orElse(null);
	}

	private static PolymorphicDefinition fromAnnotation(Polymorphic anno) {
		if (anno == null) return null;

		Map<String, Class<?>> map = new LinkedHashMap<>();
		for (Polymorphic.Type t : anno.mappings())
			map.put(t.value(), t.target());

		LinkedHashMap<String, Class<?>> infer = new LinkedHashMap<>();
		for (Polymorphic.Infer inf : anno.inferBy())
			infer.put(inf.field(), inf.target());

		Class<?> defaultTarget = anno.defaultTarget();

		return new PolymorphicDefinition(
				anno.discriminator(),
				Map.copyOf(map),
				anno.defaultValue(),
				infer,
				defaultTarget
		);
	}
}

