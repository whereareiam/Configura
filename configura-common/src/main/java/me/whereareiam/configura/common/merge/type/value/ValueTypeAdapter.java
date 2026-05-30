package me.whereareiam.configura.common.merge.type.value;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in adapter for scalar-like value fields.
 */
public final class ValueTypeAdapter implements MergeTypeAdapter {
	@Override
	public boolean supports(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
		Class<?> declaredType = descriptor.getDeclaredType();
		return declaredType.isPrimitive()
				|| declaredType.isEnum()
				|| CharSequence.class.isAssignableFrom(declaredType)
				|| Number.class.isAssignableFrom(declaredType)
				|| declaredType == Boolean.class
				|| declaredType == Character.class;
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
