package me.whereareiam.configura.feature.comment;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import me.whereareiam.configura.document.DocumentWrite;
import me.whereareiam.configura.document.DocumentWriteHook;
import me.whereareiam.configura.feature.comment.document.CommentResolver;
import me.whereareiam.configura.feature.comment.document.DocumentComments;
import me.whereareiam.configura.feature.comment.generator.CommentingGenerator;
import me.whereareiam.configura.feature.comment.generator.CommentingYamlFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Writes the comments of a document's model. A YAML document with comments is written through a
 * generator of this hook's own, copied from the mapper's factory; anything else through the rest of
 * the chain.
 */
final class CommentWriteHook implements DocumentWriteHook {
	private volatile Copy yaml;

	@Override
	public @NotNull JsonGenerator open(@NotNull DocumentWrite write, @NotNull Next next) throws IOException {
		if (write.getModelType() == null) return next.open();

		DocumentComments comments = new CommentResolver(write.getDocuments()).resolve(write.getDocument(), write.getModelType());
		if (comments.isEmpty()) return next.open();

		if (write.getMapper().getFactory() instanceof YAMLFactory factory) {
			JsonGenerator generator = yamlFactory(factory).createGenerator(write.getOutput());
			generator.setCodec(write.getMapper());
			return CommentingGenerator.wrap(generator, comments);
		}

		return CommentingGenerator.wrap(next.open(), comments);
	}

	private CommentingYamlFactory yamlFactory(YAMLFactory factory) {
		Copy copy = yaml;
		if (copy != null && copy.source() == factory) return copy.factory();

		copy = new Copy(factory, new CommentingYamlFactory(factory));
		yaml = copy;

		return copy.factory();
	}

	/** The commenting copy of the factory a mapper writes YAML with. */
	private record Copy(YAMLFactory source, CommentingYamlFactory factory) {
	}
}
