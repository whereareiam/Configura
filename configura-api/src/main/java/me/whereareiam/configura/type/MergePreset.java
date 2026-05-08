package me.whereareiam.configura.type;

import me.whereareiam.configura.merge.MergePolicy;

public enum MergePreset {
	DEEP_DEFAULTS,
	SOURCE_OWNS_FIELD,
	SOURCE_OWNS_MAP,
	SOURCE_OWNS_LIST,
	NEVER_TEMPLATE,
	DECLARED_KEYS_ONLY_MAP;

	public MergePolicy policy() {
		return switch (this) {
			case DEEP_DEFAULTS -> MergePolicy.builder().build();
			case SOURCE_OWNS_FIELD -> MergePolicy.builder()
					.valueMode(MergePolicy.ValueMode.SOURCE_OWNS_VALUE)
					.objectMode(MergePolicy.ObjectMode.SOURCE_OWNS_OBJECT)
					.mapMode(MergePolicy.MapMode.SOURCE_OWNS_MAP)
					.listMode(MergePolicy.ListMode.SOURCE_OWNS_LIST)
					.build();
			case SOURCE_OWNS_MAP -> MergePolicy.builder()
					.mapMode(MergePolicy.MapMode.SOURCE_OWNS_MAP)
					.build();
			case SOURCE_OWNS_LIST -> MergePolicy.builder()
					.listMode(MergePolicy.ListMode.SOURCE_OWNS_LIST)
					.build();
			case NEVER_TEMPLATE -> MergePolicy.builder()
					.valueMode(MergePolicy.ValueMode.NEVER_TEMPLATE)
					.listMode(MergePolicy.ListMode.NEVER_TEMPLATE)
					.build();
			case DECLARED_KEYS_ONLY_MAP -> MergePolicy.builder()
					.mapMode(MergePolicy.MapMode.DECLARED_SOURCE_KEYS_ONLY)
					.build();
		};
	}
}
