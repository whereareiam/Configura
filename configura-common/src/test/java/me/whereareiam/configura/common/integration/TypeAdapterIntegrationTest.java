package me.whereareiam.configura.common.integration;

import me.whereareiam.configura.Config;
import me.whereareiam.configura.TypeAdapter;
import me.whereareiam.configura.annotation.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeAdapterIntegrationTest {
	public static class Dur {
		public long seconds;

		static Dur parse(String v) {
			Dur duration = new Dur();
			duration.seconds = Long.parseLong(v.replaceAll("\\D", ""));

			return duration;
		}
	}

	public static class DurAdapter implements TypeAdapter<Dur> {
		public Dur deserialize(String value) {
			return Dur.parse(value);
		}

		public String serialize(Dur value) {
			return "PT" + value.seconds + "S";
		}
	}

	static class Job {
		@Field
		public Dur timeout;
	}

	@Test
	void adapterRoundtripWithConfig(@TempDir Path dir) {
		Config.registerAdapter(Dur.class, DurAdapter.class);

		Job job = new Job();
		job.timeout = new Dur();
		job.timeout.seconds = 45;

		String file = dir.resolve("job.yaml").toString();
		Config.save(file, job);

		Job read = Config.load(file, Job.class);
		assertEquals(45, read.timeout.seconds);
	}
}


