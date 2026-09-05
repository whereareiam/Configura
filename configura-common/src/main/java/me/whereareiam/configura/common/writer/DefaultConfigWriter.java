package me.whereareiam.configura.common.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.common.MapperFactory;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public final class DefaultConfigWriter implements ConfigWriter {
	private final String extension;
	private final ObjectMapper mapper;

	public DefaultConfigWriter() {
		this(Format.YAML);
	}

	public DefaultConfigWriter(Format format) {
		this(format.getExtension(), MapperFactory.buildWriterMapper(format));
	}

	@Override
	public <T> void write(Path path, T config) {
		Path target = resolve(path);
		try {
			ensureParent(target);
			writeAtomically(target, mapper.writeValueAsBytes(config));
		} catch (IOException e) {
			throw new ConfigException("Failed to write config file: " + target, e);
		}
	}

	@Override
	public <T> byte[] writeBytes(T config) {
		try {
			return mapper.writeValueAsBytes(config);
		} catch (IOException e) {
			throw new ConfigException("Failed to serialize config to bytes", e);
		}
	}

	@Override
	public void writeNode(Path path, JsonNode node) {
		Path target = resolve(path);
		try {
			ensureParent(target);
			writeAtomically(target, mapper.writeValueAsBytes(node != null ? node : mapper.createObjectNode()));
		} catch (IOException e) {
			throw new ConfigException("Failed to write config tree: " + target, e);
		}
	}

	@Override
	public byte[] writeNodeBytes(JsonNode node) {
		try {
			return mapper.writeValueAsBytes(node != null ? node : mapper.createObjectNode());
		} catch (IOException e) {
			throw new ConfigException("Failed to write config tree bytes", e);
		}
	}

	private void writeAtomically(Path target, byte[] bytes) throws IOException {
		Path absolute = target.toAbsolutePath();
		Path temporary = Files.createTempFile(absolute.getParent(), ".configura-", ".tmp");
		try {
			try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(temporary,
					java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
				java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
				while (buffer.hasRemaining()) channel.write(buffer);
				channel.force(true);
			}
			if (Files.exists(absolute) && Files.getFileStore(absolute).supportsFileAttributeView("posix"))
				Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(absolute));
			Files.move(temporary, absolute, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private Path resolve(Path path) {
		return FileUtil.resolvePathWithExtension(path, extension);
	}

	private void ensureParent(Path path) throws IOException {
		Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
	}
}
