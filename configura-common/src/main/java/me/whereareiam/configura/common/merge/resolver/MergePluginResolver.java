package me.whereareiam.configura.common.merge.resolver;

import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.merge.plugin.MergePlugin;
import me.whereareiam.configura.merge.plugin.MergePluginRegistry;
import me.whereareiam.configura.merge.plugin.descriptor.MergeDescriptor;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

@RequiredArgsConstructor
public final class MergePluginResolver {
	private final MergePluginRegistry pluginRegistry;
	private final MergePolicyResolverRegistry policyResolverRegistry;

	public @NotNull ResolvedField resolve(@NotNull Class<?> ownerType, @NotNull Field field) {
		return resolve(ownerType, field, SerializedFieldResolver.resolveSerializedName(field));
	}

	public @NotNull ResolvedField resolve(@NotNull Class<?> ownerType, @NotNull String serializedName) {
		Field field = SerializedFieldResolver.resolveField(ownerType, serializedName);
		return resolve(ownerType, field, serializedName);
	}

	private @NotNull ResolvedField resolve(
			@NotNull Class<?> ownerType,
			@Nullable Field field,
			@NotNull String serializedName
	) {
		Class<?> declaredType = SerializedFieldResolver.resolveDeclaredType(field, ownerType);
		Type genericType = field != null ? field.getGenericType() : declaredType;
		MergeDescriptor descriptor = new MergeDescriptor(
				ownerType,
				field,
				serializedName,
				declaredType,
				genericType
		);
		MergePolicy policy = policyResolverRegistry.resolve(descriptor);
		MergePlugin plugin = pluginRegistry.resolve(descriptor, policy);
		Class<?> childType = plugin.resolveChildType(descriptor, policy);
		return new ResolvedField(descriptor, policy, plugin, childType);
	}

	public static final class ResolvedField {
		private final MergeDescriptor descriptor;
		private final MergePolicy policy;
		private final MergePlugin plugin;
		private final Class<?> childType;

		private ResolvedField(
				MergeDescriptor descriptor,
				MergePolicy policy,
				MergePlugin plugin,
				Class<?> childType
		) {
			this.descriptor = descriptor;
			this.policy = policy;
			this.plugin = plugin;
			this.childType = childType;
		}

		public MergeDescriptor descriptor() {
			return descriptor;
		}

		public MergePolicy policy() {
			return policy;
		}

		public MergePlugin plugin() {
			return plugin;
		}

		public Class<?> childType() {
			return childType;
		}
	}
}
