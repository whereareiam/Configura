package me.whereareiam.configura.feature.extension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.whereareiam.configura.document.DocumentTypeContext;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ExtensionFeatureModule extends SimpleModule {
	private final ExtensionRuleTypeResolver resolver;
	private final ObjectMapper plainMapper;

	public ExtensionFeatureModule(ExtensionRuleTypeResolver resolver, ObjectMapper plainMapper) {
		super("configura-extension-feature-module", Version.unknownVersion());
		this.resolver = resolver;
		this.plainMapper = plainMapper;
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addBeanDeserializerModifier(new BeanDeserializerModifier() {
			@Override
			public JsonDeserializer<?> modifyDeserializer(
					DeserializationConfig config,
					BeanDescription beanDesc,
					JsonDeserializer<?> deserializer
			) {
				Class<?> raw = beanDesc.getBeanClass();
				if (!resolver.hasRules(raw))
					return deserializer;
				return new ExtensionAwareDeserializer(raw, deserializer, resolver, plainMapper, null, null);
			}
		});
	}

	private static final class ExtensionAwareDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
		private static final ThreadLocal<Deque<ResolutionState>> STATE = ThreadLocal.withInitial(ArrayDeque::new);

		private final Class<?> declaredType;
		private final JsonDeserializer<?> delegate;
		private final ExtensionRuleTypeResolver resolver;
		private final ObjectMapper plainMapper;
		private final Field containingField;
		private final String fieldName;

		private ExtensionAwareDeserializer(
				Class<?> declaredType,
				JsonDeserializer<?> delegate,
				ExtensionRuleTypeResolver resolver,
				ObjectMapper plainMapper,
				Field containingField,
				String fieldName
		) {
			this.declaredType = declaredType;
			this.delegate = delegate;
			this.resolver = resolver;
			this.plainMapper = plainMapper;
			this.containingField = containingField;
			this.fieldName = fieldName;
		}

		@Override
		public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
			JsonNode node = parser.readValueAsTree();
			ResolutionState parentState = STATE.get().peek();
			DocumentTypeContext documentContext = new DocumentTypeContext(
					node,
					parentState != null ? parentState.node : null,
					containingField,
					fieldName,
					parser.getParsingContext() != null ? parser.getParsingContext().getCurrentName() : null,
					null
			);
			Class<?> effectiveType = resolver.resolve(declaredType, documentContext);
			ResolutionState currentState = new ResolutionState(node);
			STATE.get().push(currentState);
			try {
				if (effectiveType == null || effectiveType == declaredType)
					return plainMapper.treeToValue(node, declaredType);
				ObjectMapper mapper = (ObjectMapper) parser.getCodec();
				return mapper.treeToValue(node, effectiveType);
			} catch (Exception exception) {
				throw new IOException("Failed to bind extendable document " + declaredType.getName(), exception);
			} finally {
				STATE.get().pop();
				if (STATE.get().isEmpty())
					STATE.remove();
			}
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext context, @Nullable BeanProperty property) {
			Field field = resolveField(property);
			String contextualFieldName = property != null ? property.getName() : fieldName;
			JsonDeserializer<?> contextualDelegate = delegate;
			if (delegate instanceof ContextualDeserializer contextualDeserializer) {
				try {
					contextualDelegate = contextualDeserializer.createContextual(context, property);
				} catch (com.fasterxml.jackson.databind.JsonMappingException exception) {
					throw new IllegalStateException("Failed to contextualize extendable document deserializer", exception);
				}
			}
			return new ExtensionAwareDeserializer(declaredType, contextualDelegate, resolver, plainMapper, field, contextualFieldName);
		}

		private static @Nullable Field resolveField(@Nullable BeanProperty property) {
			if (property == null) return null;
			AnnotatedMember member = property.getMember();
			if (member == null || member.getMember() == null) return null;
			return member.getMember() instanceof Field field ? field : null;
		}
	}

	private record ResolutionState(JsonNode node) {
	}
}
