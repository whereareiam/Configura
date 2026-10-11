package me.whereareiam.configura.feature.comment;

import me.whereareiam.configura.ConfiguraFeature;
import me.whereareiam.configura.document.DocumentWriteHook;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Writes the {@link me.whereareiam.configura.feature.comment.api.annotation.Comment}s of a model into the files
 * Configura writes: {@code #} comments in YAML, {@code //} comments in indented JSON.
 *
 * <pre>{@code
 * Configura configura = Configura.builder()
 * 		.feature(CommentFeature.defaults())
 * 		.build();
 * }</pre>
 */
public final class CommentFeature implements ConfiguraFeature {
	private final CommentWriteHook hook = new CommentWriteHook();

	public static @NotNull CommentFeature defaults() {
		return new CommentFeature();
	}

	@Override
	public @NotNull List<DocumentWriteHook> writeHooks() {
		return List.of(hook);
	}
}
