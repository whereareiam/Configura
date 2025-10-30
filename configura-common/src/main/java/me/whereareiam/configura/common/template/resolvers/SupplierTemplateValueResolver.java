package me.whereareiam.configura.common.template.resolvers;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.configura.annotation.template.Supplier;
import me.whereareiam.configura.common.template.TemplateValueResolver;

import java.lang.reflect.Field;

public final class SupplierTemplateValueResolver implements TemplateValueResolver {
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object resolve(ObjectMapper mapper, Class<?> targetType, Field field) {
		Supplier supplierAnn = field.getAnnotation(Supplier.class);
		if (supplierAnn == null) return null;

		TemplateProvider<?> provider = instantiate(supplierAnn.value());

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


