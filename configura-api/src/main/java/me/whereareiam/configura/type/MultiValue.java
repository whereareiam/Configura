package me.whereareiam.configura.type;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a value that may be specified as either a single element or multiple elements.
 *
 * @param <T> element type
 */
@Getter
@ToString
@EqualsAndHashCode
@SuppressWarnings("unused")
public class MultiValue<T> {
	private final List<T> values;
	private final boolean singlePreferred;

	public MultiValue() {
		this.values = new ArrayList<>();
		this.singlePreferred = false;
	}

	public MultiValue(T single) {
		this.values = new ArrayList<>();
		if (single != null)
			this.values.add(single);
		this.singlePreferred = true;
	}

	public MultiValue(Collection<T> values) {
		this(values, false);
	}

	public MultiValue(Collection<T> values, boolean singlePreferred) {
		this.values = values != null ? new ArrayList<>(values) : new ArrayList<>();
		this.singlePreferred = singlePreferred;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public List<T> asList() {
		return List.copyOf(values);
	}

	public T firstOrNull() {
		return values.isEmpty() ? null : values.get(0);
	}

	public static <T> MultiValue<T> of(T single) {
		return new MultiValue<>(single);
	}

	public static <T> MultiValue<T> of(Collection<T> values) {
		return new MultiValue<>(values, false);
	}
}
