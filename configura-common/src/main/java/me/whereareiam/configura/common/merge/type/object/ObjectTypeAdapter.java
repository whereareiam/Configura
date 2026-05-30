package me.whereareiam.configura.common.merge.type.object;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Built-in adapter for ordinary object fields.
 */
public final class ObjectTypeAdapter implements MergeTypeAdapter {
	@Override
	public boolean supports(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
		Class<?> declaredType = descriptor.getDeclaredType();
		return !declaredType.isPrimitive()
				&& !declaredType.isEnum()
				&& !CharSequence.class.isAssignableFrom(declaredType)
				&& !Number.class.isAssignableFrom(declaredType)
				&& declaredType != Boolean.class
				&& declaredType != Character.class
				&& !Map.class.isAssignableFrom(declaredType)
				&& !List.class.isAssignableFrom(declaredType)
				&& !Collection.class.isAssignableFrom(declaredType);
	}

	@Override
	public @NotNull Class<?> resolveChildType(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
		return descriptor.getDeclaredType();
	}

	@Override
	public @NotNull JsonNode merge(@NotNull MergeTypeAdapterContext context) {
		MergeContext mergeContext = new MergeContext(
				context.getMapper(),
				context.getDescriptor().getOwnerType(),
				context.getDescriptor().getField(),
				context.getDescriptor().getSerializedName(),
				context.getChildType(),
				context.getSourceNode(),
				context.getDefaultNode(),
				context.sourceTreatsDefaultAsMissing(),
				context.getBehavior(),
				context.getRecursiveMerge()
		);
		return context.requireStrategy().merge(mergeContext);
	}
}
