package me.whereareiam.configura.common.merge.resolver;

import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.common.util.SerializedFieldResolver;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.MergeTypeAdapterRegistry;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

@RequiredArgsConstructor
public final class MergeTypeAdapterResolver {
	private final MergeTypeAdapterRegistry adapterRegistry;
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
		MergeTypeDescriptor descriptor = new MergeTypeDescriptor(
				ownerType,
				field,
				serializedName,
				declaredType,
				genericType
		);
		MergePolicy policy = policyResolverRegistry.resolve(descriptor);
		MergeTypeAdapter adapter = adapterRegistry.resolve(descriptor, policy);
		Class<?> childType = adapter.resolveChildType(descriptor, policy);

		return new ResolvedField(descriptor, policy, adapter, childType);
	}

	public record ResolvedField(
			@NotNull MergeTypeDescriptor descriptor,
			@NotNull MergePolicy policy,
			@NotNull MergeTypeAdapter adapter,
			@NotNull Class<?> childType
	) {
	}
}
