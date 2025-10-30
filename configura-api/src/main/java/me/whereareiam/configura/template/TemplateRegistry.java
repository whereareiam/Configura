package me.whereareiam.configura.template;

import me.whereareiam.configura.TemplateProvider;

/**
 * API surface to register and retrieve template providers for model types.
 */
public interface TemplateRegistry {
	<T, P extends TemplateProvider<T>> void registerTemplate(Class<P> providerClass);

	<T> TemplateProvider<T> getTemplateProvider(Class<T> modelType);
}


