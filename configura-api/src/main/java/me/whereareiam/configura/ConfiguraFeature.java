package me.whereareiam.configura;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.whereareiam.configura.document.DocumentPhase;
import me.whereareiam.configura.document.DocumentTypeResolver;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Contributes optional Configura behavior such as type resolution, document phases, or mapper
 * modules.
 */
public interface ConfiguraFeature {
	/**
	 * Returns additional mapper modules contributed by this feature.
	 *
	 * @param plainMapper plain mapper before feature modules are added
	 * @return contributed mapper modules
	 */
	default @NotNull List<Module> modules(@NotNull ObjectMapper plainMapper) {
		return List.of();
	}

	/**
	 * Returns document type resolvers contributed by this feature.
	 *
	 * @return contributed type resolvers
	 */
	default @NotNull List<DocumentTypeResolver> typeResolvers() {
		return List.of();
	}

	/**
	 * Returns document phases contributed by this feature.
	 *
	 * @return contributed document phases
	 */
	default @NotNull List<DocumentPhase> phases() {
		return List.of();
	}
}
