package me.whereareiam.configura.common.template.resolver.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.common.template.resolver.TemplateValueResolver;

import java.lang.reflect.Field;

public final class SupplierTemplateValueResolver implements TemplateValueResolver {
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {

		Template t = field.getAnnotation(Template.class);
		if (t == null || t.supplier() == null || t.supplier().value() == null
				|| t.supplier().value() == Template.Supplier.None.class) {
			return null;
		}

		Class<? extends TemplateProvider<?>> providerClass = t.supplier().value();

		TemplateProvider<?> provider = instantiate(providerClass);

		Object instance = newInstance(targetType);
		return ((TemplateProvider) provider).supply(instance);
	}

	private static TemplateProvider<?> instantiate(Class<? extends TemplateProvider<?>> cls) {
		try {
			return cls.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate template provider: " + cls.getName(), e);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object newInstance(Class targetType) {
		try {
			return targetType.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate template target: " + targetType.getName(), e);
		}
	}
}


