package me.whereareiam.configura.merge.defaults.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.merge.defaults.MergeModelDefaultsResolver;
import me.whereareiam.configura.merge.defaults.descriptor.DefaultsDescriptor;
import me.whereareiam.configura.type.PrimitiveDefaultPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime context passed to a defaults resolver.
 */
@RequiredArgsConstructor
public final class DefaultsContext {
	private final @NotNull ObjectMapper mapper;
	private final @NotNull DefaultsDescriptor descriptor;
	private final @NotNull Class<?> childType;
	private final @NotNull PrimitiveDefaultPolicy primitiveDefaultPolicy;
	private final @NotNull MergeModelDefaultsResolver modelDefaultsResolver;

	public @NotNull ObjectMapper getMapper() {
		return mapper;
	}

	public @NotNull DefaultsDescriptor getDescriptor() {
		return descriptor;
	}

	public @NotNull Class<?> getChildType() {
		return childType;
	}

	public @NotNull PrimitiveDefaultPolicy getPrimitiveDefaultPolicy() {
		return primitiveDefaultPolicy;
	}

	public @Nullable JsonNode resolveModelDefaults(@NotNull Class<?> type) {
		return modelDefaultsResolver.resolve(type);
	}

	public @Nullable JsonNode resolveModelDefaults(
			@NotNull Class<?> type,
			@Nullable DocumentTypeContext context
	) {
		return modelDefaultsResolver.resolve(type, context);
	}
}
