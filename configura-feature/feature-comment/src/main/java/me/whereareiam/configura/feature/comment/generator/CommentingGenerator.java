package me.whereareiam.configura.feature.comment.generator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.json.JsonGeneratorImpl;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import me.whereareiam.configura.feature.comment.document.DocumentComments;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wraps a generator and writes the {@link DocumentComments} of a document while the document is
 * serialized through it: the header before the root value, every other comment before its key.
 */
public final class CommentingGenerator extends JsonGeneratorDelegate {
	private final DocumentComments comments;
	private final Sink sink;
	private boolean headerWritten;

	private CommentingGenerator(JsonGenerator generator, DocumentComments comments, Sink sink) {
		super(generator, false);
		this.comments = comments;
		this.sink = sink;
	}

	/**
	 * Wraps the generator if its format can carry comments: YAML generators of a
	 * {@link CommentingYamlFactory}, and JSON generators that pretty-print. Any other generator, or
	 * a document without comments, is returned as it is.
	 */
	public static JsonGenerator wrap(JsonGenerator generator, DocumentComments comments) {
		if (comments.isEmpty()) return generator;

		Sink sink = sink(generator);
		if (sink == null) return generator;

		return new CommentingGenerator(generator, comments, sink);
	}

	private static @Nullable Sink sink(JsonGenerator generator) {
		if (generator instanceof CommentingYamlFactory.Generator yaml)
			return new Sink() {
				@Override
				public void header(List<String> lines) throws IOException {
					yaml.writeHeader(lines);
				}

				@Override
				public void key(List<String> lines) throws IOException {
					yaml.writeComment(lines);
				}
			};

		if (!(generator instanceof JsonGeneratorImpl) || !(generator.getPrettyPrinter() instanceof DefaultPrettyPrinter base))
			return null;

		CommentingPrettyPrinter printer = new CommentingPrettyPrinter(base);
		generator.setPrettyPrinter(printer);

		return new Sink() {
			@Override
			public void header(List<String> lines) throws IOException {
				for (String line : lines)
					generator.writeRaw((line.isEmpty() ? "//" : "// " + line) + "\n");
				generator.writeRaw("\n");
			}

			@Override
			public void key(List<String> lines) {
				printer.comment(lines);
			}
		};
	}

	@Override
	public void writeStartObject() throws IOException {
		writeHeader();
		super.writeStartObject();
	}

	@Override
	public void writeStartObject(Object forValue) throws IOException {
		writeHeader();
		super.writeStartObject(forValue);
	}

	@Override
	public void writeStartObject(Object forValue, int size) throws IOException {
		writeHeader();
		super.writeStartObject(forValue, size);
	}

	@Override
	public void writeFieldName(String name) throws IOException {
		writeKeyComment(name);
		super.writeFieldName(name);
	}

	@Override
	public void writeFieldName(SerializableString name) throws IOException {
		writeKeyComment(name.getValue());
		super.writeFieldName(name);
	}

	private void writeHeader() throws IOException {
		if (headerWritten || !getOutputContext().inRoot()) return;

		headerWritten = true;
		List<String> lines = comments.at(List.of());
		if (!lines.isEmpty()) sink.header(lines);
	}

	private void writeKeyComment(String name) throws IOException {
		List<String> path = path();
		path.add(name);

		List<String> lines = comments.at(path);
		if (!lines.isEmpty()) sink.key(lines);
	}

	/** The keys and list positions leading to the object currently written. */
	private List<String> path() {
		List<String> path = new ArrayList<>();
		for (JsonStreamContext context = getOutputContext(); context.getParent() != null; context = context.getParent()) {
			JsonStreamContext parent = context.getParent();
			if (parent.inObject()) path.add(parent.getCurrentName());
			else if (parent.inArray()) path.add(String.valueOf(parent.getCurrentIndex()));
		}
		Collections.reverse(path);

		return path;
	}

	private interface Sink {
		void header(List<String> lines) throws IOException;

		void key(List<String> lines) throws IOException;
	}
}
