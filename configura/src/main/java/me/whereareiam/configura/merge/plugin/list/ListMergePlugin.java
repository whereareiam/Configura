package me.whereareiam.configura.merge.plugin.list;

import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.plugin.MergePlugin;
import me.whereareiam.configura.merge.plugin.context.MergePluginContext;
import me.whereareiam.configura.merge.plugin.context.MergePluginDefaultsContext;
import me.whereareiam.configura.merge.plugin.descriptor.MergeDescriptor;
import me.whereareiam.configura.merge.plugin.property.PropertyMergeStrategyHandler;
import me.whereareiam.configura.merge.policy.MergePolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Built-in list property plugin used by Configura.
 */
public final class ListMergePlugin implements MergePlugin {
	private final ListDefaultsResolver defaultsResolver = new ListDefaultsResolver();

	@Override
	public boolean supports(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
		return descriptor.getField() != null && supportsList(descriptor.getField());
	}

	@Override
	public @NotNull Class<?> resolveChildType(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
		Field field = descriptor.getField();
		if (field == null) return descriptor.getDeclaredType();
		return resolveEntryType(field, descriptor.getDeclaredType());
	}

	@Override
	public @Nullable JsonNode resolveDefaultValue(@NotNull MergePluginDefaultsContext context) {
		Field field = context.getDescriptor().getField();
		if (field == null) return null;
		return defaultsResolver.resolve(context.getMapper(), field);
	}

	@Override
	public @NotNull JsonNode merge(@NotNull MergePluginContext context) {
		Field field = context.getDescriptor().getField();
		ListMergeConfig config = context.getPolicy().helper(ListMergeConfig.class);
		if (config == null) {
			return new PropertyMergeStrategyHandler(context.getMapper()).merge(
					context.getDescriptor().getOwnerType(),
					field,
					context.getDescriptor().getSerializedName(),
					context.getDescriptor().getDeclaredType(),
					context.getSourceNode(),
					context.getDefaultNode(),
					context.getBehavior(),
					context.sourceTreatsDefaultAsMissing(),
					context.getStrategyClass(),
					context.getRecursiveMerge()
			);
		}
		JsonNode genericDefaults = context.resolveModelDefaults(context.getChildType());
		return new ListMergeCoordinator(context.getMapper()).merge(
				field,
				context.getSourceNode(),
				context.getDefaultNode(),
				genericDefaults,
				context.getChildType(),
				context.getStrategyClass(),
				context.getBehavior(),
				config,
				context.getRecursiveMerge()
		);
	}

	private static boolean supportsList(Field field) {
		return field != null && Collection.class.isAssignableFrom(field.getType());
	}

	private static @NotNull Class<?> resolveEntryType(Field field, Class<?> fallback) {
		Type genericType = field.getGenericType();
		if (!(genericType instanceof ParameterizedType parameterizedType))
			return fallback;

		Type[] arguments = parameterizedType.getActualTypeArguments();
		if (arguments.length < 1 || !(arguments[0] instanceof Class<?> entryType))
			return fallback;

		return entryType;
	}
}
