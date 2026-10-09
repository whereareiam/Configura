package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.AnnotationDefaultsResolver;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.configura.merge.defaults.DefaultsResolver;
import me.whereareiam.configura.merge.defaults.DefaultsResolverRegistry;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.MergeTypeAdapterRegistry;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestMergeProperties {
	public static @NotNull MergeTypeAdapterRegistry adapterRegistry() {
		return new MergeTypeAdapterRegistry().register(new TestPropertyTypeAdapter());
	}

	public static @NotNull DefaultsResolverRegistry defaultsResolverRegistry() {
		return new DefaultsResolverRegistry().register(new AnnotationDefaultsResolver());
	}

	public static @NotNull MergePolicyResolverRegistry policyResolverRegistry() {
		return new MergePolicyResolverRegistry();
	}

	private static final class TestPropertyTypeAdapter implements MergeTypeAdapter {
		@Override
		public boolean supports(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
			return true;
		}

		@Override
		public @NotNull Class<?> resolveChildType(@NotNull MergeTypeDescriptor descriptor, @NotNull MergePolicy policy) {
			Field field = descriptor.getField();
			if (field == null) return descriptor.getDeclaredType();
			if (java.util.Collection.class.isAssignableFrom(field.getType())) return resolveCollectionEntryType(field, descriptor.getDeclaredType());
			return descriptor.getDeclaredType();
		}

		@Override
		public @NotNull JsonNode merge(@NotNull MergeTypeAdapterContext context) {
			JsonNode source = context.getSourceNode();
			JsonNode defaults = context.getDefaultNode();
			if (source == null || source.isNull() || context.sourceTreatsDefaultAsMissing())
				return defaults == null ? context.getMapper().nullNode() : defaults.deepCopy();
			if (source.isObject() && defaults != null && defaults.isObject())
				return context.mergeChildren(source, defaults);
			return source.deepCopy();
		}

		private @NotNull Class<?> resolveCollectionEntryType(Field field, Class<?> fallback) {
			Type genericType = field.getGenericType();
			if (!(genericType instanceof ParameterizedType parameterizedType))
				return fallback;
			Type[] arguments = parameterizedType.getActualTypeArguments();
			if (arguments.length < 1 || !(arguments[0] instanceof Class<?> entryType))
				return fallback;
			return entryType;
		}
	}
}
