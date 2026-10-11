package me.whereareiam.configura.feature.comment.generator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import me.whereareiam.configura.feature.comment.document.DocumentComments;

import java.io.IOException;
import java.util.List;

/**
 * Writes pending comment lines of a JSON document between the separator of an object entry and its
 * key, so each comment sits on its own line above the key it describes.
 */
final class CommentingPrettyPrinter extends DefaultPrettyPrinter {
	private List<String> pending = List.of();

	CommentingPrettyPrinter(DefaultPrettyPrinter base) {
		super(base);
	}

	@Override
	public CommentingPrettyPrinter createInstance() {
		return new CommentingPrettyPrinter(this);
	}

	void comment(List<String> lines) {
		pending = lines;
	}

	@Override
	public void beforeObjectEntries(JsonGenerator generator) throws IOException {
		super.beforeObjectEntries(generator);
		writePending(generator);
	}

	@Override
	public void writeObjectEntrySeparator(JsonGenerator generator) throws IOException {
		super.writeObjectEntrySeparator(generator);
		writePending(generator);
	}

	private void writePending(JsonGenerator generator) throws IOException {
		if (pending.isEmpty()) return;

		for (String line : pending) {
			if (_objectIndenter.isInline()) {
				generator.writeRaw("/* " + line.replace("*/", "* /") + " */ ");
				continue;
			}

			generator.writeRaw(line.isEmpty() ? "//" : "// " + line);
			_objectIndenter.writeIndentation(generator, _nesting);
		}

		pending = List.of();
	}
}
