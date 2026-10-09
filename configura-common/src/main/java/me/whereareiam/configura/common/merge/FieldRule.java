package me.whereareiam.configura.common.merge;

import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.type.merge.MissingEntries;
import me.whereareiam.configura.type.merge.UnknownEntries;
import me.whereareiam.configura.type.merge.WhenAbsent;
import me.whereareiam.configura.type.merge.WhenPresent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * How one field is merged: what its {@link Merge} annotation says, or the defaults without one.
 */
final class FieldRule {
	private static final FieldRule UNANNOTATED = new FieldRule(
			null,
			WhenAbsent.ADD_DEFAULT,
			WhenPresent.FILL_MISSING,
			"",
			MissingEntries.ADD,
			UnknownEntries.ALLOW
	);

	private final @Nullable Field field;
	private final WhenAbsent absent;
	private final WhenPresent present;
	private final String key;
	private final MissingEntries missingEntries;
	private final UnknownEntries unknownEntries;

	private FieldRule(
			@Nullable Field field,
			WhenAbsent absent,
			WhenPresent present,
			String key,
			MissingEntries missingEntries,
			UnknownEntries unknownEntries
	) {
		this.field = field;
		this.absent = absent;
		this.present = present;
		this.key = key;
		this.missingEntries = missingEntries;
		this.unknownEntries = unknownEntries;
	}

	static FieldRule of(@Nullable Field field) {
		if (field == null) return UNANNOTATED;

		Merge merge = field.getAnnotation(Merge.class);
		if (merge == null)
			return new FieldRule(field, UNANNOTATED.absent, UNANNOTATED.present, "", UNANNOTATED.missingEntries, UNANNOTATED.unknownEntries);

		return new FieldRule(field, merge.absent(), merge.present(), merge.key(), merge.missingEntries(), merge.unknownEntries());
	}

	boolean addsDefaultWhenAbsent() {
		return absent == WhenAbsent.ADD_DEFAULT;
	}

	boolean fillsMissing() {
		return present == WhenPresent.FILL_MISSING;
	}

	boolean addsMissingEntries() {
		return missingEntries == MissingEntries.ADD;
	}

	boolean rejectsUnknownEntries() {
		return unknownEntries == UnknownEntries.REJECT;
	}

	boolean isMap() {
		return field != null && Map.class.isAssignableFrom(field.getType());
	}

	boolean isKeyedList() {
		return field != null && List.class.isAssignableFrom(field.getType()) && !key.isEmpty();
	}

	String key() {
		return key;
	}
}
