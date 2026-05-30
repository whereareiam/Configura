package me.whereareiam.configura.common;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.ConfiguraFeature;
import me.whereareiam.configura.document.DocumentPhase;
import me.whereareiam.configura.document.DocumentTypeResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ConfiguraFeatureRegistry {
	private final List<ConfiguraFeature> features = new ArrayList<>();

	public @NotNull ConfiguraFeatureRegistry add(@NotNull ConfiguraFeature contributor) {
		features.add(contributor);
		return this;
	}

	public @NotNull List<ConfiguraFeature> features() {
		return List.copyOf(features);
	}

	public @NotNull List<Module> modules(@NotNull ObjectMapper plainMapper) {
		List<Module> modules = new ArrayList<>();
		for (ConfiguraFeature feature : features)
			modules.addAll(feature.modules(plainMapper));
		return List.copyOf(modules);
	}

	public @NotNull List<DocumentTypeResolver> typeResolvers() {
		List<DocumentTypeResolver> resolvers = new ArrayList<>();
		for (ConfiguraFeature feature : features)
			resolvers.addAll(feature.typeResolvers());
		return List.copyOf(resolvers);
	}

	public @NotNull List<DocumentPhase> phases() {
		List<DocumentPhase> phases = new ArrayList<>();
		for (ConfiguraFeature feature : features)
			phases.addAll(feature.phases());
		return List.copyOf(phases);
	}
}
