package me.whereareiam.configura.feature.comment.generator;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker;
import me.whereareiam.configura.feature.comment.document.DocumentComments;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.events.CommentEvent;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * A {@link YAMLFactory} whose generators can write comment lines. Without comments its output is
 * that of the factory it was copied from.
 */
public final class CommentingYamlFactory extends YAMLFactory {
	/** Creates a factory that writes YAML exactly as the given factory does, plus comments. */
	public CommentingYamlFactory(YAMLFactory source) {
		super(source, null);
	}

	private CommentingYamlFactory(CommentingYamlFactory source, ObjectCodec codec) {
		super(source, codec);
	}

	@Override
	public CommentingYamlFactory copy() {
		_checkInvalidCopy(CommentingYamlFactory.class);
		return new CommentingYamlFactory(this, null);
	}

	@Override
	protected YAMLGenerator _createGenerator(Writer out, IOContext context) throws IOException {
		return new Generator(context, _generatorFeatures, _yamlGeneratorFeatures, _quotingChecker, _objectCodec, out, _version);
	}

	static final class Generator extends YAMLGenerator {
		private Generator(
				IOContext context,
				int features,
				int yamlFeatures,
				StringQuotingChecker quotingChecker,
				ObjectCodec codec,
				Writer out,
				DumperOptions.Version version
		) throws IOException {
			super(context, features, yamlFeatures, quotingChecker, codec, out, version);
		}

		@Override
		protected DumperOptions buildDumperOptions(int features, int yamlFeatures, DumperOptions.Version version) {
			DumperOptions options = super.buildDumperOptions(features, yamlFeatures, version);
			options.setProcessComments(true);

			return options;
		}

		/** Writes the lines as block comments above what is written next. */
		void writeComment(List<String> lines) throws IOException {
			for (String line : lines)
				_emit(new CommentEvent(CommentType.BLOCK, line.isEmpty() ? "" : " " + line, null, null));
		}

		/** Writes the lines as the header of the document, set apart by an empty line. */
		void writeHeader(List<String> lines) throws IOException {
			writeComment(lines);
			_emit(new CommentEvent(CommentType.BLANK_LINE, "", null, null));
		}
	}
}
