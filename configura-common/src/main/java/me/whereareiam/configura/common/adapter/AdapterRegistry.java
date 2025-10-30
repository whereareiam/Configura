package me.whereareiam.configura.common.adapter;

import me.whereareiam.configura.TypeAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AdapterRegistry {
	private final Map<Class<?>, Class<? extends TypeAdapter<?>>> classMap;

	private AdapterRegistry(Map<Class<?>, Class<? extends TypeAdapter<?>>> classMap) {
		this.classMap = Map.copyOf(classMap);
	}

	public static AdapterRegistry empty() {
		return new AdapterRegistry(Collections.emptyMap());
	}

	public <T> AdapterRegistry withAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		Map<Class<?>, Class<? extends TypeAdapter<?>>> next = new HashMap<>(classMap);
		next.put(type, adapterClass);
		return new AdapterRegistry(next);
	}

	public Map<Class<?>, Class<? extends TypeAdapter<?>>> asClassMap() {
		return classMap;
	}

	public Map<Class<?>, TypeAdapter<?>> instantiate() {
		Map<Class<?>, TypeAdapter<?>> instances = new HashMap<>();
		for (Map.Entry<Class<?>, Class<? extends TypeAdapter<?>>> e : classMap.entrySet()) {
			try {
				@SuppressWarnings("unchecked")
				Class<? extends TypeAdapter<Object>> cls = (Class<? extends TypeAdapter<Object>>) e.getValue();
				instances.put(e.getKey(), cls.getDeclaredConstructor().newInstance());
			} catch (Exception ex) {
				throw new IllegalStateException("Failed to instantiate adapter: " + e.getValue().getName(), ex);
			}
		}
		return instances;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof AdapterRegistry that && Objects.equals(classMap, that.classMap);
	}

	@Override
	public int hashCode() {
		return Objects.hash(classMap);
	}
}


