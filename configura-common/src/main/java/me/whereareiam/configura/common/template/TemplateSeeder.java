package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.common.util.BeanPropertyUtil;
import me.whereareiam.configura.common.util.PathNavigator;
import me.whereareiam.configura.template.TemplateRegistry;

import java.lang.reflect.Field;
import java.util.List;

public final class TemplateSeeder {
	private final ObjectMapper mapper;
	private final TemplateRegistry templateRegistry;

	public TemplateSeeder(ObjectMapper mapper, TemplateRegistry templateRegistry) {
		this.mapper = mapper;
		this.templateRegistry = templateRegistry;
	}

	public <T> T seed(T model) {
		if (model == null) return null;
		ObjectNode node = mapper.valueToTree(model);
		seedRootFromModelProvider(node, model.getClass());
		seedNode(node, model.getClass());
		try {
			return mapper.readerForUpdating(model).readValue(node);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to bind seeded node", e);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void seedRootFromModelProvider(ObjectNode target, Class<?> type) {
		if (templateRegistry == null) return;
		try {
			TemplateProvider<?> provider = templateRegistry.getTemplateProvider((Class) type);
			if (provider == null) return;

			Object instance = type.getDeclaredConstructor().newInstance();
			Object provided = ((TemplateProvider) provider).supply(instance);
			if (provided == null) return;

			ObjectNode defaults = mapper.valueToTree(provided);
			mergeMissing(target, defaults);
		} catch (Exception ignored) {
		}
	}

	private static void mergeMissing(ObjectNode target, ObjectNode defaults) {
		for (var entry : defaults.properties()) {
			String key = entry.getKey();
			var value = entry.getValue();
			var existing = target.get(key);

			if (existing == null || existing.isNull()) {
				target.set(key, value);
				continue;
			}

			if (existing.isObject() && value.isObject())
				mergeMissing((ObjectNode) existing, (ObjectNode) value);
		}
	}

	private void seedNode(ObjectNode node, Class<?> type) {
		BeanDescription desc = mapper.getDeserializationConfig().introspect(mapper.constructType(type));
		List<BeanPropertyDefinition> props = desc.findProperties();
		if (props == null || props.isEmpty()) return;

		for (BeanPropertyDefinition prop : props) {
			String key = BeanPropertyUtil.computeKey(prop);
			if (key == null || key.isEmpty()) continue;
			if (PathNavigator.has(node, key)) continue;

			AnnotatedMember member = prop.getPrimaryMember();
			if (member == null) continue;

			Field f = member.getMember() instanceof Field ? (Field) member.getMember() : null;
			Object templateValue = f == null ? null : TemplateResolver.resolveFieldTemplate(mapper, templateRegistry, member.getRawType(), f);

			if (templateValue != null) {
				PathNavigator.write(node, key, mapper.valueToTree(templateValue));
				continue;
			}

			Class<?> raw = member.getRawType();
			if (hasInnerTemplates(raw)) {
				ObjectNode child = mapper.createObjectNode();
				PathNavigator.write(node, key, child);
				seedNode(child, raw);
			}
		}
	}

	private boolean hasInnerTemplates(Class<?> nestedType) {
		BeanDescription desc = mapper.getDeserializationConfig().introspect(mapper.constructType(nestedType));
		List<BeanPropertyDefinition> props = desc.findProperties();

		if (props == null || props.isEmpty()) return false;
		for (BeanPropertyDefinition prop : props) {
			AnnotatedMember member = prop.getPrimaryMember();
			if (member == null) continue;

			Field f = member.getMember() instanceof Field ? (Field) member.getMember() : null;
			if (f == null) continue;

			Object v = TemplateResolver.resolveFieldTemplate(mapper, templateRegistry, member.getRawType(), f);
			if (v != null) return true;
		}

		return false;
	}
}


