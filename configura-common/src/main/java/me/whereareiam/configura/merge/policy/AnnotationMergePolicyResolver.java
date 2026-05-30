package me.whereareiam.configura.merge.policy;

import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.annotation.merge.MergeList;
import me.whereareiam.configura.annotation.merge.MergeMap;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.merge.plugin.list.ListMergeConfig;
import me.whereareiam.configura.merge.plugin.map.MapMergeConfig;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import me.whereareiam.configura.type.merge.tree.list.ListMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Built-in annotation-backed property policy resolver used by Configura.
 */
public final class AnnotationMergePolicyResolver implements MergePolicyResolver {
	@Override
	public @Nullable MergePolicy resolve(@NotNull MergeTypeDescriptor descriptor) {
		Field field = descriptor.getField();
		if (field == null) return null;

		MergePolicy.Builder builder = MergePolicy.builder();
		Merge merge = field.getAnnotation(Merge.class);
		if (merge != null) {
			if (!merge.named().isBlank()) builder.namedStrategy(merge.named());
			else if (!FieldMergeStrategy.class.equals(merge.value())) builder.strategy(merge.value());
		}

		MergeMap map = field.getAnnotation(MergeMap.class);
		if (map != null) {
			builder.helper(MapMergeConfig.class, new MapMergeConfig(
					map.presence(),
					map.unknownEntries()
			));
		}

		MergeList list = field.getAnnotation(MergeList.class);
		if (list != null) {
			if (list.mode() == ListMode.KEYED && list.key().isBlank()) {
				throw new ConfigException("Field " + describe(field) + " uses keyed list merge but no key was configured");
			}

			builder.helper(ListMergeConfig.class, new ListMergeConfig(
					list.mode(),
					list.key(),
					list.presence(),
					list.unknownEntries()
			));
		}

		MergePolicy policy = builder.build();
		return policy.isEmpty() ? null : policy;
	}

	private static String describe(Field field) {
		return field.getDeclaringClass().getName() + "#" + field.getName();
	}
}
