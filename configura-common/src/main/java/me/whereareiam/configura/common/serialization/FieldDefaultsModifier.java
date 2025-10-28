package me.whereareiam.configura.common.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.common.template.TemplateResolver;
import me.whereareiam.configura.common.util.PathNavigator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies @Field defaults, env variables, and required checks during deserialization.
 */
public final class FieldDefaultsModifier extends BeanDeserializerModifier {
	private final Set<Class<?>> adaptedTypes;

	public FieldDefaultsModifier(Map<Class<?>, TypeAdapter<?>> adapters) {
		this.adaptedTypes = adapters == null ? Collections.emptySet() : Set.copyOf(adapters.keySet());
	}

	@Override
	public JsonDeserializer<?> modifyDeserializer(
			DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer
	) {
		if (beanDesc.getBeanClass().getName().startsWith("java.")) return deserializer;
		if (adaptedTypes.contains(beanDesc.getBeanClass())) return deserializer;

		List<BeanPropertyDefinition> props = beanDesc.findProperties();
		if (props == null || props.isEmpty()) return deserializer;

		return new ConfigFieldAwareDeserializer(deserializer, props);
	}

	private static final class ConfigFieldAwareDeserializer extends DelegatingDeserializer {
		private final List<BeanPropertyDefinition> properties;

		ConfigFieldAwareDeserializer(
				JsonDeserializer<?> delegate,
				List<BeanPropertyDefinition> properties
		) {
			super(delegate);
			this.properties = properties;
		}

		@Override
		protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
			return new ConfigFieldAwareDeserializer(newDelegatee, properties);
		}

		@Override
		public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			ObjectCodec codec = p.getCodec();
			if (codec == null) return _delegatee.deserialize(p, ctxt);
			JsonNode node = codec.readTree(p);
			ObjectNode obj = node != null && node.isObject() ? (ObjectNode) node : JsonNodeFactory.instance.objectNode();

			for (BeanPropertyDefinition prop : properties) {
				AnnotatedMember member = prop.getPrimaryMember();
				if (member == null) continue;
				Field cf = member.getAnnotation(Field.class);
				String key = prop.getName();
				if (cf != null) {
					if (!cf.name().isEmpty()) key = cf.name();
				}
				if (!PathNavigator.has(obj, key)) {
					try {
						java.lang.reflect.Field f = member.getMember() instanceof java.lang.reflect.Field ? (java.lang.reflect.Field) member.getMember() : null;
						if (f != null) {
							Object tpl = TemplateResolver.resolveFieldTemplate((ObjectMapper) codec, member.getRawType(), f);
							if (tpl != null) {
								JsonNode nodeVal = ((ObjectMapper) codec).valueToTree(tpl);
								PathNavigator.write(obj, key, nodeVal);
							}
						}
					} catch (Exception ignored) {
					}
				}
			}

			JsonParser reparsed = obj.traverse(codec);
			reparsed.nextToken();
			return _delegatee.deserialize(reparsed, ctxt);
		}
	}
}



