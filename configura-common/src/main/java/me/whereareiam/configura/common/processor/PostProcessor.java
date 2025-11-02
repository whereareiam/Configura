package me.whereareiam.configura.common.processor;

import me.whereareiam.configura.annotation.PostProcess;
import me.whereareiam.configura.exception.ConfigException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processor for handling {@link PostProcess} annotated methods in configuration objects.
 * <p>
 * This class discovers and invokes methods annotated with {@code @PostProcess} after
 * a configuration object has been loaded. It manages tracking of "once" methods to ensure
 * they are only executed on the first load.
 */
public final class PostProcessor {
	private static final Set<String> processedOnceKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/**
	 * Processes all {@link PostProcess} annotated methods in the given configuration object.
	 *
	 * @param config the configuration object
	 * @param <T>    the type of the configuration object
	 */
	public static <T> void process(T config) {
		if (config == null) return;

		Class<?> clazz = config.getClass();
		List<MethodInfo> methodInfos = collectPostProcessMethods(clazz);

		for (MethodInfo info : methodInfos) {
			if (info.once) {
				String key = generateOnceKey(config, info.method);
				if (processedOnceKeys.contains(key))
					continue; // Skip if already processed

				processedOnceKeys.add(key);
			}

			invokeMethod(config, info.method);
		}
	}

	private static List<MethodInfo> collectPostProcessMethods(Class<?> clazz) {
		List<MethodInfo> methods = new ArrayList<>();

		// Traverse up the class hierarchy
		Class<?> current = clazz;
		while (current != null && current != Object.class) {
			for (Method method : current.getDeclaredMethods()) {
				PostProcess annotation = method.getAnnotation(PostProcess.class);
				if (annotation != null) {
					validateMethod(method);
					methods.add(new MethodInfo(method, annotation.once()));
				}
			}
			current = current.getSuperclass();
		}

		return methods;
	}

	private static void validateMethod(Method method) {
		if (method.getParameterCount() != 0) {
			throw new ConfigException(
					"@PostProcess method must have no parameters: " + method.getName()
			);
		}

		if (!method.getReturnType().equals(void.class)) {
			throw new ConfigException(
					"@PostProcess method must return void: " + method.getName()
			);
		}

		if (!Modifier.isPublic(method.getModifiers())) {
			throw new ConfigException(
					"@PostProcess method must be public: " + method.getName()
			);
		}
	}

	private static void invokeMethod(Object target, Method method) {
		try {
			method.setAccessible(true);
			method.invoke(target);
		} catch (Exception e) {
			throw new ConfigException(
					"Failed to invoke @PostProcess method: " + method.getName(),
					e
			);
		}
	}

	private static String generateOnceKey(Object config, Method method) {
		return config.getClass().getName() + "#" + method.getName();
	}

	private static class MethodInfo {
		final Method method;
		final boolean once;

		MethodInfo(Method method, boolean once) {
			this.method = method;
			this.once = once;
		}
	}
}

