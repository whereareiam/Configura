package me.whereareiam.configura;

import com.fasterxml.jackson.core.JsonGenerator;
import me.whereareiam.configura.document.DocumentWrite;
import me.whereareiam.configura.document.DocumentWriteHook;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentWriteHookTest {
	@TempDir
	Path directory;

	@Test
	void hooksSeeTheModelOfEveryWrite() throws Exception {
		List<String> seen = new ArrayList<>();
		Configura configura = Configura.builder()
				.feature(feature((write, next) -> {
					seen.add(write.getModelType() == null ? "none" : write.getModelType().getSimpleName());
					return next.open();
				}))
				.build();
		Path file = directory.resolve("settings.yml");

		configura.update(file, Settings.class);
		configura.save(file, new Settings());
		configura.write(file, new Settings());
		configura.writeBytes(new Settings());
		configura.writeNode(file, configura.readNode(file));

		assertEquals(List.of("Settings", "Settings", "Settings", "Settings", "none"), seen);
	}

	@Test
	void hooksRunInRegistrationOrderAndLeaveTheOutputAsItWas() {
		List<String> order = new ArrayList<>();
		Configura configura = Configura.builder()
				.feature(feature((write, next) -> {
					order.add("first");
					return next.open();
				}))
				.feature(feature((write, next) -> {
					order.add("second");
					return next.open();
				}))
				.build();

		byte[] written = configura.writeBytes(new Settings());

		assertEquals(List.of("first", "second"), order);
		assertArrayEquals(Config.yaml().writeBytes(new Settings()), written);
	}

	@Test
	void aHookCanWriteThroughItsOwnGenerator() throws Exception {
		Configura configura = Configura.builder()
				.feature(feature((write, next) -> write.getMapper().getFactory().createGenerator(write.getOutput())))
				.build();
		Path file = directory.resolve("settings.yml");

		configura.update(file, Settings.class);

		assertEquals("name: \"lobby\"\n", Files.readString(file));
	}

	private static ConfiguraFeature feature(DocumentWriteHook hook) {
		return new ConfiguraFeature() {
			@Override
			public @NotNull List<DocumentWriteHook> writeHooks() {
				return List.of(hook);
			}
		};
	}

	public static class Settings {
		public String name = "lobby";
	}
}
