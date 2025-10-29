package me.whereareiam.configura.common.writer;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.annotation.Policy;
import me.whereareiam.configura.common.AdapterRegistry;
import me.whereareiam.configura.common.serialization.MapperFactory;
import me.whereareiam.configura.common.util.FileUtil;
import me.whereareiam.configura.common.util.PathNavigator;
import me.whereareiam.configura.exception.ConfigException;
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.reader.ConfigReaderProvider;
import me.whereareiam.configura.type.Format;
import me.whereareiam.configura.writer.ConfigWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;

public class DefaultConfigWriter implements ConfigWriter {
	private final Format format;
	private final AdapterRegistry registry;
	private final ObjectMapper mapper;

	public DefaultConfigWriter() {
		this.format = Format.YAML;
		this.registry = AdapterRegistry.empty();
		this.mapper = MapperFactory.buildMapper(this.format, this.registry);
	}

	@Override
	public ConfigWriter withFormat(Format format) {
		return new DefaultConfigWriter(format, this.registry);
	}

	@Override
	public <T> ConfigWriter registerAdapter(Class<T> type, Class<? extends TypeAdapter<T>> adapterClass) {
		AdapterRegistry next = this.registry.withAdapter(type, adapterClass);
		return new DefaultConfigWriter(this.format, next);
	}

	@Override
	public <T> void save(String filePath, T config) {
		String resolved = FileUtil.resolvePathWithFormat(filePath, format);
		try {
			Path path = Path.of(resolved);
			Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
			mapper.writeValue(path.toFile(), config);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + resolved, e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T updateRead(String filePath, T config) {
		Class<T> configClass = (Class<T>) config.getClass();

		String resolved = FileUtil.resolvePathWithFormat(filePath, format);
		Path path = Path.of(resolved);

		ObjectNode existing = readExistingNode(path, resolved);
		ObjectNode modelNode = toNode(config);
		ObjectNode merged = mergePerField(configClass, existing, modelNode);
		writeNode(path, resolved, merged);

		ConfigReader reader = buildReaderWithAdapters();
		return reader.load(filePath, configClass);
	}

	@Override
	public Format getFormat() {
		return format;
	}

	private DefaultConfigWriter(Format format, AdapterRegistry registry) {
		this.format = format;
		this.registry = registry;
		this.mapper = MapperFactory.buildMapper(this.format, this.registry);
	}

	private ObjectNode readExistingNode(Path path, String resolved) {
		ObjectNode existing = mapper.createObjectNode();
		if (Files.exists(path)) {
			try {
				JsonNode n = mapper.readTree(Files.newBufferedReader(path));
				if (n != null && n.isObject()) existing = (ObjectNode) n;
			} catch (IOException e) {
				throw new ConfigException("Failed to read config file: " + resolved, e);
			}
		}
		return existing;
	}

	private <T> ObjectNode toNode(T config) {
		return mapper.valueToTree(config);
	}

	private ObjectNode mergePerField(Class<?> configClass, ObjectNode existing, ObjectNode modelNode) {
		Policy classPolicy = configClass.getAnnotation(Policy.class);
		boolean classMerge = classPolicy == null || classPolicy.mergeOnUpdate();

		ObjectNode target = mapper.createObjectNode();
		BeanDescription desc = mapper.getSerializationConfig().introspect(mapper.constructType(configClass));
		for (BeanPropertyDefinition prop : desc.findProperties()) {
			String name = prop.getName();
			AnnotatedMember member = prop.getPrimaryMember();
			if (member == null) continue;

			Policy fieldPolicy = member.getAnnotation(Policy.class);
			boolean merge = fieldPolicy != null ? fieldPolicy.mergeOnUpdate() : classMerge;

			JsonNode modelValue = modelNode.get(name);
			JsonNode existingValue = PathNavigator.read(existing, name);

			if (merge) {
				if (modelValue != null) PathNavigator.write(target, name, modelValue);
				continue;
			}

			if (existingValue != null) {
				PathNavigator.write(target, name, existingValue);
				continue;
			}

			if (modelValue != null) {
				PathNavigator.write(target, name, modelValue);
			}
		}

		return target;
	}

	private void writeNode(Path path, String resolved, ObjectNode node) {
		try {
			Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
			mapper.writeValue(path.toFile(), node);
		} catch (IOException e) {
			throw new ConfigException("Failed to save config file: " + resolved, e);
		}
	}

	@SuppressWarnings("unchecked")
	private ConfigReader buildReaderWithAdapters() {
		ConfigReader reader = createReader().withFormat(format);
		for (Map.Entry<Class<?>, Class<? extends TypeAdapter<?>>> e : registry.asClassMap().entrySet()) {
			Class<Object> cls = (Class<Object>) e.getKey();
			Class<? extends TypeAdapter<Object>> adapterCls = (Class<? extends TypeAdapter<Object>>) e.getValue();
			reader = reader.registerAdapter(cls, adapterCls);
		}

		return reader;
	}

	private static ConfigReader createReader() {
		for (ConfigReaderProvider p : ServiceLoader.load(ConfigReaderProvider.class)) {
			return p.create();
		}
		throw new UnsupportedOperationException("No ConfigReaderProvider found. Add configura-common to the classpath.");
	}
}