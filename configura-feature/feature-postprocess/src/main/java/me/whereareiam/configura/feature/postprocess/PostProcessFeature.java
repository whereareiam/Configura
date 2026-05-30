package me.whereareiam.configura.feature.postprocess;

import me.whereareiam.configura.ConfiguraFeature;
import me.whereareiam.configura.document.DocumentPhase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PostProcessFeature implements ConfiguraFeature {
	private final PostProcessPhase phase = new PostProcessPhase();

	public static @NotNull PostProcessFeature defaults() {
		return new PostProcessFeature();
	}

	@Override
	public @NotNull List<DocumentPhase> phases() {
		return List.of(phase);
	}
}
