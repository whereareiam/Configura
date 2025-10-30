package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.annotation.Field;
import me.whereareiam.configura.common.util.PathNavigator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Jackson module that wires template-driven default injection.
 */
public final class TemplateModule extends SimpleModule {
	private final Map<Class<?>, TypeAdapter<?>> adapters;

	public TemplateModule() {
		this(null);
	}

	public TemplateModule(Map<Class<?>, TypeAdapter<?>> adapters) {
		super("configura-template-module", Version.unknownVersion());
		this.adapters = adapters;
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		final Set<Class<?>> adaptedTypes = adapters == null ? Collections.emptySet() : Set.copyOf(adapters.keySet());

		context.addBeanDeserializerModifier(new InjectionModifier(adaptedTypes));
	}

	private static final class InjectionModifier extends BeanDeserializerModifier {
		private final Set<Class<?>> adaptedTypes;

		InjectionModifier(Set<Class<?>> adaptedTypes) {
			this.adaptedTypes = adaptedTypes;
		}

		@Override
		public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
			if (shouldSkip(beanDesc.getBeanClass(), adaptedTypes)) return deserializer;

			List<BeanPropertyDefinition> properties = beanDesc.findProperties();
			if (properties == null || properties.isEmpty()) return deserializer;

			return new TemplateAwareDeserializer(deserializer, properties);
		}
	}

	private static final class TemplateAwareDeserializer extends DelegatingDeserializer {
		private final List<BeanPropertyDefinition> properties;

		TemplateAwareDeserializer(JsonDeserializer<?> delegate, List<BeanPropertyDefinition> properties) {
			super(delegate);
			this.properties = properties;
		}

		@Override
		protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
			return new TemplateAwareDeserializer(newDelegatee, properties);
		}

		@Override
		public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			return injectAndDeserialize(p, ctxt, properties, _delegatee);
		}
	}

	private static boolean shouldSkip(Class<?> raw, Set<Class<?>> adaptedTypes) {
		return raw.getName().startsWith("java.") || adaptedTypes.contains(raw);
	}

	private static String computeKey(BeanPropertyDefinition prop) {
		AnnotatedMember member = prop.getPrimaryMember();
		if (member == null) return prop.getName();

		Field cf = member.getAnnotation(Field.class);
		if (cf != null && !cf.name().isEmpty()) return cf.name();

		return prop.getName();
	}

	private static void injectDefaults(
			ObjectNode obj, List<BeanPropertyDefinition> properties, ObjectMapper mapper
	) {
		for (BeanPropertyDefinition prop : properties) {
			AnnotatedMember member = prop.getPrimaryMember();
			if (member == null) continue;

			String key = computeKey(prop);
			if (PathNavigator.has(obj, key)) continue;

			try {
				java.lang.reflect.Field f = member.getMember() instanceof java.lang.reflect.Field ? (java.lang.reflect.Field) member.getMember() : null;
				if (f == null) continue;

				Object tpl = TemplateResolver.resolveFieldTemplate(mapper, member.getRawType(), f);
				if (tpl == null) continue;

				JsonNode nodeVal = mapper.valueToTree(tpl);
				PathNavigator.write(obj, key, nodeVal);
			} catch (Exception ignored) {
			}
		}
	}

	private static Object injectAndDeserialize(
			JsonParser p, DeserializationContext ctxt, List<BeanPropertyDefinition> properties, JsonDeserializer<?> delegate
	) throws IOException {
		ObjectCodec codec = p.getCodec();

		if (!(codec instanceof ObjectMapper mapper)) return delegate.deserialize(p, ctxt);

		JsonNode node = codec.readTree(p);
		ObjectNode obj = node != null && node.isObject() ? (ObjectNode) node : JsonNodeFactory.instance.objectNode();
		injectDefaults(obj, properties, mapper);
		JsonParser reparsed = obj.traverse(codec);
		reparsed.nextToken();

		return delegate.deserialize(reparsed, ctxt);
	}
}


