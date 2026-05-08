package me.whereareiam.configura.merge;

public final class MergePolicy {
	public enum ValueMode {
		TEMPLATE_WHEN_MISSING,
		SOURCE_OWNS_VALUE,
		NEVER_TEMPLATE
	}

	public enum ObjectMode {
		DEEP_FILL,
		SOURCE_OWNS_OBJECT
	}

	public enum MapMode {
		DEEP_FILL_ALL_KEYS,
		SOURCE_OWNS_MAP,
		DECLARED_SOURCE_KEYS_ONLY
	}

	public enum ListMode {
		TEMPLATE_WHEN_MISSING,
		SOURCE_OWNS_LIST,
		NEVER_TEMPLATE
	}

	private final ValueMode valueMode;
	private final ObjectMode objectMode;
	private final MapMode mapMode;
	private final ListMode listMode;
	private final boolean preserveExplicitNull;

	private MergePolicy(Builder builder) {
		this.valueMode = builder.valueMode;
		this.objectMode = builder.objectMode;
		this.mapMode = builder.mapMode;
		this.listMode = builder.listMode;
		this.preserveExplicitNull = builder.preserveExplicitNull;
	}

	public static Builder builder() {
		return new Builder();
	}

	public ValueMode valueMode() {
		return valueMode;
	}

	public ObjectMode objectMode() {
		return objectMode;
	}

	public MapMode mapMode() {
		return mapMode;
	}

	public ListMode listMode() {
		return listMode;
	}

	public boolean preserveExplicitNull() {
		return preserveExplicitNull;
	}

	public static final class Builder {
		private ValueMode valueMode = ValueMode.TEMPLATE_WHEN_MISSING;
		private ObjectMode objectMode = ObjectMode.DEEP_FILL;
		private MapMode mapMode = MapMode.DEEP_FILL_ALL_KEYS;
		private ListMode listMode = ListMode.TEMPLATE_WHEN_MISSING;
		private boolean preserveExplicitNull = true;

		public Builder valueMode(ValueMode valueMode) {
			this.valueMode = valueMode;
			return this;
		}

		public Builder objectMode(ObjectMode objectMode) {
			this.objectMode = objectMode;
			return this;
		}

		public Builder mapMode(MapMode mapMode) {
			this.mapMode = mapMode;
			return this;
		}

		public Builder listMode(ListMode listMode) {
			this.listMode = listMode;
			return this;
		}

		public Builder preserveExplicitNull(boolean preserveExplicitNull) {
			this.preserveExplicitNull = preserveExplicitNull;
			return this;
		}

		public MergePolicy build() {
			return new MergePolicy(this);
		}
	}
}
