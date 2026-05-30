package me.whereareiam.configura.feature.postprocess;

import me.whereareiam.configura.document.DocumentPhase;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.feature.postprocess.api.PostProcess;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PostProcessPhase implements DocumentPhase {
	private final Set<String> processedOnceKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Override
	public void afterBind(@NotNull Object value) {
		for (MethodInfo info : collectPostProcessMethods(value.getClass())) {
			if (info.once && !shouldInvokeOnce(value, info.method)) continue;
			invokeMethod(value, info.method);
		}
	}

	private boolean shouldInvokeOnce(Object value, Method method) {
		String key = value.getClass().getName() + "#" + method.getName();
		if (processedOnceKeys.contains(key)) return false;

		processedOnceKeys.add(key);
		return true;
	}

	private List<MethodInfo> collectPostProcessMethods(Class<?> type) {
		List<MethodInfo> methods = new ArrayList<>();
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			for (Method method : current.getDeclaredMethods()) {
				PostProcess annotation = method.getAnnotation(PostProcess.class);
				if (annotation == null) continue;
				validateMethod(method);
				methods.add(new MethodInfo(method, annotation.once()));
			}
		}
		return methods;
	}

	private void validateMethod(Method method) {
		if (method.getParameterCount() != 0) throw new ConfigException("@PostProcess method must have no parameters: " + method.getName());
		if (!void.class.equals(method.getReturnType())) throw new ConfigException("@PostProcess method must return void: " + method.getName());
		if (!Modifier.isPublic(method.getModifiers())) throw new ConfigException("@PostProcess method must be public: " + method.getName());
	}

	private void invokeMethod(Object target, Method method) {
		try {
			method.setAccessible(true);
			method.invoke(target);
		} catch (Exception exception) {
			throw new ConfigException("Failed to invoke @PostProcess method: " + method.getName(), exception);
		}
	}

	private record MethodInfo(Method method, boolean once) {
	}
}
