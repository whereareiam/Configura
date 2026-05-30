package me.whereareiam.configura.common.merge.type.map;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.plugin.map.MapMergeConfig;
import me.whereareiam.configura.merge.plugin.map.MapMergeCoordinator;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.type.BuiltinStrategyCapabilities;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import me.whereareiam.configura.type.merge.tree.map.MapPresence;
import me.whereareiam.configura.type.merge.tree.map.MapUnknownEntries;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Built-in adapter for map fields.
 */
public final class MapTypeAdapter implements MergeTypeAdapter {
	private static final MapMergeConfig DEFAULT_CONFIG = new MapMergeConfig(
			MapPresence.SEED_DEFAULTS,
			MapUnknownEntries.ALLOW
	);

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
		return new MapMergeCoordinator(context.getMapper()).merge(
				field,
				context.getSourceNode(),
				context.getDefaultNode(),
				context.getChildType(),
				context.capability(BuiltinStrategyCapabilities.MAP),
				context.getBehavior(),
				config != null ? config : DEFAULT_CONFIG,
				context
		);
	}
}
