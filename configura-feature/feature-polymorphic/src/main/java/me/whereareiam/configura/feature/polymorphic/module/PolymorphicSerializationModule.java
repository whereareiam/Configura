package me.whereareiam.configura.feature.polymorphic.module;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.feature.polymorphic.DefaultPolymorphicRegistry;
import me.whereareiam.configura.feature.polymorphic.PolymorphicTypeResolver;
import me.whereareiam.configura.feature.polymorphic.api.model.PolymorphicDefinition;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Binds a polymorphic base type to the subtype its document node stands for, and writes the
 * discriminator of a value that does not carry one.
 * <p>
 * Both directions keep the serializer and deserializer Jackson built for the base type and hand the
 * base type itself to them. Asking the mapper for the base type again would come back here.
 */
public final class PolymorphicSerializationModule extends SimpleModule {
	private final PolymorphicTypeResolver resolver;

	public PolymorphicSerializationModule(DefaultPolymorphicRegistry registry) {
		super("configura-polymorphic-feature-module", Version.unknownVersion());
		this.resolver = new PolymorphicTypeResolver(registry);
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addBeanDeserializerModifier(new BeanDeserializerModifier() {
			@Override
			public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
				Class<?> raw = beanDesc.getBeanClass();
				PolymorphicDefinition definition = resolver.definition(raw);
				if (definition == null) return deserializer;

				return new PolymorphicDeserializer(deserializer, raw, definition);
			}
		});
		context.addBeanSerializerModifier(new BeanSerializerModifier() {
			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
				PolymorphicDefinition definition = resolver.definition(beanDesc.getBeanClass());
				if (definition == null) return serializer;

				return new PolymorphicSerializer(serializer, definition);
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

	private final class PolymorphicDeserializer extends DelegatingDeserializer {
		private final Class<?> raw;
		private final PolymorphicDefinition definition;

		private PolymorphicDeserializer(JsonDeserializer<?> delegate, Class<?> raw, PolymorphicDefinition definition) {
			super(delegate);
			this.raw = raw;
			this.definition = definition;
		}

		@Override
		protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
			return new PolymorphicDeserializer(newDelegatee, raw, definition);
		}

		@Override
		public Object deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
			JsonPointer location = parser.getParsingContext().pathAsPointer();
			JsonNode node = parser.readValueAsTree();
			Class<?> target = resolver.target(definition, node);
			if (target != null && target != raw)
				return ((ObjectMapper) parser.getCodec()).treeToValue(node, target);

			if (target == null) {
				String written = PolymorphicTypeResolver.discriminatorValue(definition, node);
				if (written != null && !written.isEmpty())
					throw new ConfigException(unknownValue(written, location));
				if (raw.isInterface() || Modifier.isAbstract(raw.getModifiers()))
					throw new ConfigException(undecided(location));
			}

			// The base type is its own target, so its own deserializer binds it.
			JsonParser tree = node.traverse(parser.getCodec());
			tree.nextToken();
			return _delegatee.deserialize(tree, ctxt);
		}

		private String unknownValue(String written, JsonPointer location) {
			return "Unknown '" + definition.getDiscriminator() + "' value '" + written + "' for " + raw.getName()
					+ at(location) + "; accepted values: " + String.join(", ", definition.getMappings().keySet());
		}

		private String undecided(JsonPointer location) {
			String subject = "Cannot choose a subtype of " + raw.getName() + at(location) + ": ";
			String discriminator = definition.getDiscriminator();
			if (discriminator != null && !discriminator.isEmpty())
				return subject + "'" + discriminator + "' is missing; accepted values: "
						+ String.join(", ", definition.getMappings().keySet());

			if (definition.getInferFields() == null || definition.getInferFields().isEmpty())
				return subject + "its definition names no discriminator and no fields to tell by";

			return subject + "it has none of the fields " + String.join(", ", definition.getInferFields().keySet());
		}

		private static String at(JsonPointer location) {
			String path = location.toString();
			return path.isEmpty() ? "" : " at " + path;
		}
	}

	private static final class PolymorphicSerializer extends JsonSerializer<Object> implements ContextualSerializer, ResolvableSerializer {
		private final JsonSerializer<Object> delegate;
		private final PolymorphicDefinition definition;

		@SuppressWarnings("unchecked")
		private PolymorphicSerializer(JsonSerializer<?> delegate, PolymorphicDefinition definition) {
			this.delegate = (JsonSerializer<Object>) delegate;
			this.definition = definition;
		}

		@Override
		public void serialize(Object value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
			TokenBuffer buffer = new TokenBuffer(generator.getCodec(), false);
			delegate.serialize(value, buffer, serializers);
			JsonNode node = buffer.asParser(generator.getCodec()).readValueAsTree();

			String discriminator = definition.getDiscriminator();
			if (discriminator != null && !discriminator.isEmpty() && node instanceof ObjectNode object && !object.has(discriminator)) {
				String inferred = inferDiscriminatorValue(definition, value);
				if (inferred != null) object.put(discriminator, inferred);
			}
			generator.writeTree(node);
		}

		@Override
		public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) throws JsonMappingException {
			if (!(delegate instanceof ContextualSerializer contextual)) return this;

			JsonSerializer<?> contextualized = contextual.createContextual(provider, property);
			return contextualized == delegate ? this : new PolymorphicSerializer(contextualized, definition);
		}

		@Override
		public void resolve(SerializerProvider provider) throws JsonMappingException {
			if (delegate instanceof ResolvableSerializer resolvable) resolvable.resolve(provider);
		}
	}
}
