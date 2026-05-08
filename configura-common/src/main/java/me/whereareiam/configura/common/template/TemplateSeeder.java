package me.whereareiam.configura.common.template;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.common.merge.MergePolicyResolver;
import me.whereareiam.configura.common.util.PathNavigator;
import me.whereareiam.configura.merge.MergePolicy;
import me.whereareiam.configura.template.TemplateRegistry;

import java.lang.reflect.Field;
import java.util.List;

public final class TemplateSeeder {
	private final ObjectMapper mapper;
	private final TemplateRegistry templateRegistry;
	private final SeedingMode mode;
	private final MergePolicyResolver mergePolicyResolver;

	public enum SeedingMode {
		DEFAULT_INSTANCE, // seeding a zero/empty-constructed instance (e.g., update)
		USER_MODEL        // seeding a user-provided model (e.g., save/encode)
	}

	public TemplateSeeder(ObjectMapper mapper, TemplateRegistry templateRegistry) {
		this(mapper, templateRegistry, SeedingMode.USER_MODEL, new MergePolicyResolver());
	}

	public TemplateSeeder(ObjectMapper mapper, TemplateRegistry templateRegistry, SeedingMode mode) {
		this(mapper, templateRegistry, mode, new MergePolicyResolver());
	}

	public TemplateSeeder(ObjectMapper mapper, TemplateRegistry templateRegistry, SeedingMode mode, MergePolicyResolver mergePolicyResolver) {
		this.mapper = mapper;
		this.templateRegistry = templateRegistry;
		this.mode = mode;
		this.mergePolicyResolver = mergePolicyResolver != null ? mergePolicyResolver : new MergePolicyResolver();
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
			TemplateProvider<?> provider = getProviderForType(type);
			if (provider == null) return;

			Object instance = type.getDeclaredConstructor().newInstance();
			Object provided = ((TemplateProvider) provider).supply(instance);
			if (provided == null) return;

			ObjectNode defaults = mapper.valueToTree(provided);
			mergeMissingWithStrategy(target, defaults, type);
		} catch (Exception ignored) {
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private TemplateProvider<?> getProviderForType(Class<?> type) {
		// First check registry
		TemplateProvider<?> provider = templateRegistry.getTemplateProvider((Class) type);
		if (provider != null) return provider;
		
		// Check for class-level @Template annotation
		Template templateAnnotation = type.getAnnotation(Template.class);
		if (templateAnnotation == null) return null;
		
		Template.Supplier supplierAnnotation = templateAnnotation.supplier();
		Class<? extends TemplateProvider<?>> providerClass = supplierAnnotation.value();
		
		if (providerClass == Template.Supplier.None.class)
			return null;
		
		try {
			return providerClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	private void mergeMissing(ObjectNode target, ObjectNode defaults) {
		for (var entry : defaults.properties()) {
			String key = entry.getKey();
			var value = entry.getValue();
			var existing = target.get(key);

			boolean treatDefaultsAsMissing = shouldTreatPrimitiveDefaultAsMissing(existing);
			if (existing == null || existing.isNull() || treatDefaultsAsMissing) {
				target.set(key, value);
				continue;
			}

			if (existing.isObject() && value.isObject())
				mergeMissing((ObjectNode) existing, (ObjectNode) value);
		}
	}

	/**
	 * Merges missing values from defaults into target, respecting field-level merge strategies.
	 *
	 * @param target the target node being populated (user config)
	 * @param defaults the default values from template
	 * @param modelClass the Java class being configured
	 */
	private void mergeMissingWithStrategy(ObjectNode target, ObjectNode defaults, Class<?> modelClass) {
		for (var entry : defaults.properties()) {
			String key = entry.getKey();
			var value = entry.getValue();
			var existing = target.get(key);

			// Get the merge strategy for this field
			MergePolicy policy = mergePolicyResolver.resolve(modelClass, key);

			if (policy.valueMode() == MergePolicy.ValueMode.NEVER_TEMPLATE) {
				continue;
			}

			if (policy.valueMode() == MergePolicy.ValueMode.SOURCE_OWNS_VALUE
					|| policy.objectMode() == MergePolicy.ObjectMode.SOURCE_OWNS_OBJECT
					|| policy.mapMode() == MergePolicy.MapMode.SOURCE_OWNS_MAP
					|| policy.listMode() == MergePolicy.ListMode.SOURCE_OWNS_LIST) {
				if (existing == null || existing.isNull()) {
					target.set(key, value);
				}
				continue;
			}

			if (policy.mapMode() == MergePolicy.MapMode.DECLARED_SOURCE_KEYS_ONLY) {
				if (existing == null || existing.isNull() || shouldTreatPrimitiveDefaultAsMissing(existing)) {
					target.set(key, value);
					continue;
				}

				if (existing.isObject() && value.isObject()) {
					mergeMissing((ObjectNode) existing, (ObjectNode) value);
				}
				continue;
			}

			boolean treatDefaultsAsMissing = shouldTreatPrimitiveDefaultAsMissing(existing);
			if (existing == null || existing.isNull() || treatDefaultsAsMissing) {
				target.set(key, value);
				continue;
			}

			if (existing.isObject() && value.isObject()) {
				mergeMissing((ObjectNode) existing, (ObjectNode) value);
			}
		}
	}

	private void seedNode(ObjectNode node, Class<?> type) {
		BeanDescription desc = mapper.getDeserializationConfig().introspect(mapper.constructType(type));
		List<BeanPropertyDefinition> props = desc.findProperties();
		if (props == null || props.isEmpty()) return;

		for (BeanPropertyDefinition prop : props) {
			String key = prop.getName();
			if (key == null || key.isEmpty()) continue;

			boolean hasPath = PathNavigator.has(node, key);
			if (hasPath) {
				JsonNode existing = PathNavigator.read(node, key);
				boolean treatDefaultsAsMissing = shouldTreatPrimitiveDefaultAsMissing(existing);
				if (!treatDefaultsAsMissing) continue;
			}


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

	private boolean shouldTreatPrimitiveDefaultAsMissing(JsonNode existing) {
		if (existing == null) return false;
		if (mode != SeedingMode.DEFAULT_INSTANCE) return false;

		return (existing.isNumber() && existing.asDouble() == 0.0)
				|| (existing.isBoolean() && !existing.asBoolean())
				|| (existing.isArray() && existing.isEmpty());
	}
}
