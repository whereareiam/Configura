package me.whereareiam.configura.common.document;

import me.whereareiam.configura.document.DocumentPhase;
import me.whereareiam.configura.document.DocumentProcessor;
import me.whereareiam.configura.document.DocumentTypeContext;
import me.whereareiam.configura.document.DocumentTypeResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DefaultDocumentProcessor implements DocumentProcessor {
	private final List<DocumentTypeResolver> typeResolvers;
	private final List<DocumentPhase> phases;

	public DefaultDocumentProcessor(
			List<DocumentTypeResolver> typeResolvers,
			List<DocumentPhase> phases
	) {
		this.typeResolvers = typeResolvers != null ? List.copyOf(typeResolvers) : List.of();
		this.phases = phases != null ? List.copyOf(phases) : List.of();
	}

	@Override
	public @NotNull Class<?> resolveType(
			@NotNull Class<?> declaredType,
			@Nullable DocumentTypeContext context
	) {
		DocumentTypeContext effectiveContext = context != null
				? context
				: new DocumentTypeContext(null, null, null, null, null, null);

		for (DocumentTypeResolver resolver : typeResolvers) {
			if (resolver == null) continue;
			Class<?> resolved = resolver.resolve(declaredType, effectiveContext);
			if (resolved != null) return resolved;
		}

		return declaredType;
	}

	@Override
	public void afterBind(@Nullable Object value) {
		if (value == null) return;
		for (DocumentPhase phase : phases) {
			if (phase == null) continue;
			phase.afterBind(value);
		}
	}
}
