package me.whereareiam.configura.common.merge.type.map;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.plugin.map.MapMergeConfig;
import me.whereareiam.configura.merge.plugin.map.MapMergeCoordinator;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.type.BuiltinStrategyCapabilities;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Built-in adapter for map fields.
 */
public final class MapTypeAdapter implements MergeTypeAdapter {
	@Override
	public boolean supports(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
		return descriptor.getField() != null && Map.class.isAssignableFrom(descriptor.getField().getType());
	}

	@Override
	public @NotNull Class<?> resolveChildType(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
		Field field = descriptor.getField();
		if (field == null) return descriptor.getDeclaredType();
		Class<?> resolved = descriptor.resolveGenericArgument(1);
		return resolved != null ? resolved : descriptor.getDeclaredType();
	}

	@Override
	public @NotNull JsonNode merge(@NotNull MergeTypeAdapterContext context) {
		Field field = context.getDescriptor().getField();
		MapMergeConfig config = context.getPolicy().helper(MapMergeConfig.class);
		if (config == null) {
			MergeContext mergeContext = new MergeContext(
					context.getMapper(),
					context.getDescriptor().getOwnerType(),
					field,
					context.getDescriptor().getSerializedName(),
					context.getDescriptor().getDeclaredType(),
					context.getSourceNode(),
					context.getDefaultNode(),
					context.sourceTreatsDefaultAsMissing(),
					context.getBehavior(),
					context.getRecursiveMerge()
			);
			return context.requireStrategy().merge(mergeContext);
		}
		return new MapMergeCoordinator(context.getMapper()).merge(
				field,
				context.getSourceNode(),
				context.getDefaultNode(),
				context.getChildType(),
				context.capability(BuiltinStrategyCapabilities.MAP),
				context.getBehavior(),
				config,
				context
		);
	}
}
