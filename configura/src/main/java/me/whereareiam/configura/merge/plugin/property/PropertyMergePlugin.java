package me.whereareiam.configura.merge.plugin.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.merge.plugin.MergePlugin;
import me.whereareiam.configura.merge.plugin.context.MergePluginContext;
import me.whereareiam.configura.merge.plugin.context.MergePluginDefaultsContext;
import me.whereareiam.configura.merge.plugin.descriptor.MergeDescriptor;
import me.whereareiam.configura.merge.policy.MergePolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * Built-in fallback property plugin used by Configura.
 */
public final class PropertyMergePlugin implements MergePlugin {
	private final PropertyDefaultsResolver defaultsResolver = new PropertyDefaultsResolver();

	@Override
	public boolean supports(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
		return true;
	}

	@Override
	public @NotNull Class<?> resolveChildType(@NotNull MergeDescriptor descriptor, @NotNull MergePolicy policy) {
		return descriptor.getDeclaredType();
	}

	@Override
	public @Nullable JsonNode resolveDefaultValue(@NotNull MergePluginDefaultsContext context) {
		Field field = context.getDescriptor().getField();
		if (field == null) return null;

		JsonNode defaults = defaultsResolver.resolve(
				context.getMapper(),
				context.getDescriptor().getDeclaredType(),
				field
		);
		if (defaults != null) return defaults;
		return resolveModelDefaults(context.getDescriptor().getDeclaredType(), context);
	}

	@Override
	public @NotNull JsonNode merge(@NotNull MergePluginContext context) {
		return new PropertyMergeStrategyHandler(context.getMapper()).merge(
				context.getDescriptor().getOwnerType(),
				context.getDescriptor().getField(),
				context.getDescriptor().getSerializedName(),
				context.getChildType(),
				context.getSourceNode(),
				context.getDefaultNode(),
				context.getBehavior(),
				context.sourceTreatsDefaultAsMissing(),
				context.getStrategyClass(),
				context.getRecursiveMerge()
		);
	}

	private static @Nullable JsonNode resolveModelDefaults(Class<?> type, MergePluginDefaultsContext context) {
		if (!supportsModelDefaults(type)) return null;

		JsonNode defaults = context.resolveModelDefaults(type);
		return defaults instanceof ObjectNode ? defaults : null;
	}

	private static boolean supportsModelDefaults(Class<?> type) {
		if (type.isPrimitive() || type.isEnum()) return false;
		if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) return false;
		if (type == Object.class || type == String.class || type == Boolean.class || type == Character.class)
			return false;

		if (Number.class.isAssignableFrom(type)) return false;
		if (CharSequence.class.isAssignableFrom(type)) return false;
		if (JsonNode.class.isAssignableFrom(type)) return false;
		if (!hasNoArgsConstructor(type)) return false;

		return !List.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type);
	}

	private static boolean hasNoArgsConstructor(Class<?> type) {
		try {
			type.getDeclaredConstructor();
			return true;
		} catch (NoSuchMethodException exception) {
			return false;
		}
	}
}
