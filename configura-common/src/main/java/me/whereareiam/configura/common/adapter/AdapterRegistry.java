package me.whereareiam.configura.common.adapter;

import me.whereareiam.configura.TypeAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AdapterRegistry {
	private final Map<Class<?>, TypeAdapter<?>> adapters;

	private AdapterRegistry(Map<Class<?>, TypeAdapter<?>> adapters) {
		this.adapters = Map.copyOf(adapters);
	}

	public static AdapterRegistry empty() {
		return new AdapterRegistry(Collections.emptyMap());
	}

	public <T> AdapterRegistry withAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		// Instantiate the adapter immediately
		TypeAdapter<T> instance;
		try {
			instance = adapterClass.getDeclaredConstructor().newInstance();
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to instantiate adapter: " + adapterClass.getName(), ex);
		}
		return withAdapter(type, instance);
	}

	public <T> AdapterRegistry withAdapter(Class<T> type, TypeAdapter<T> adapterInstance) {
		Map<Class<?>, TypeAdapter<?>> next = new HashMap<>(adapters);
		next.put(type, adapterInstance);
		return new AdapterRegistry(next);
	}

	public Map<Class<?>, Class<? extends TypeAdapter<?>>> asClassMap() {
		// For backwards compatibility - build class map from instances
		Map<Class<?>, Class<? extends TypeAdapter<?>>> classMap = new HashMap<>();
		for (Map.Entry<Class<?>, TypeAdapter<?>> e : adapters.entrySet()) {
			@SuppressWarnings("unchecked")
			Class<? extends TypeAdapter<?>> adapterClass = (Class<? extends TypeAdapter<?>>) e.getValue().getClass();
			classMap.put(e.getKey(), adapterClass);
		}
		return classMap;
	}

	public Map<Class<?>, TypeAdapter<?>> instantiate() {
		return adapters;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof AdapterRegistry that && Objects.equals(adapters, that.adapters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(adapters);
	}
}


