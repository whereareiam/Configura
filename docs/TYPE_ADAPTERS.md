## Type Adapters

Type adapters let you teach Configura how to serialize/deserialize custom types. Implement `TypeAdapter<T>`, then
register it globally or per reader/writer.

### Interface

```java
public interface TypeAdapter<T> {
	T deserialize(String value) throws Exception;

	String serialize(T value) throws Exception;
}
```

### Example: Java Duration as `1h30m`

```java
import me.whereareiam.configura.TypeAdapter;

import java.time.Duration;

public class DurationAdapter implements TypeAdapter<Duration> {
	@Override
	public Duration deserialize(String value) {
		// very small parser for demo: e.g., 90s, 5m, 2h
		long totalSeconds;
		String s = value.trim();
		if (s.endsWith("s")) totalSeconds = Long.parseLong(s.substring(0, s.length() - 1));
		else if (s.endsWith("m")) totalSeconds = Long.parseLong(s.substring(0, s.length() - 1)) * 60;
		else if (s.endsWith("h")) totalSeconds = Long.parseLong(s.substring(0, s.length() - 1)) * 3600;
		else totalSeconds = Long.parseLong(s);
		return Duration.ofSeconds(totalSeconds);
	}

	@Override
	public String serialize(Duration value) {
		long seconds = value.getSeconds();
		return seconds + "s";
	}
}
```

### Register globally

Global registration applies to all static operations via `Config`.

```java
import me.whereareiam.configura.Config;

Config.registerAdapter(Duration .class, DurationAdapter .class);
```

### Register per reader/writer

If you construct readers/writers manually, you can register adapters on them:

```java
import me.whereareiam.configura.reader.ConfigReader;
import me.whereareiam.configura.writer.ConfigWriter;

ConfigReader reader = ConfigReader.create().registerAdapter(Duration.class, DurationAdapter.class);
ConfigWriter writer = ConfigWriter.create().registerAdapter(Duration.class, DurationAdapter.class);
```

### Use in a model

```java
import lombok.Data;

import java.time.Duration;

@Data
public class RateLimitConfig {
	private Duration window; // serialized as string via adapter
	private int maxRequests;
}
```

```yaml
window: "60s"
maxRequests: 100
```

### Tips

- Keep adapters deterministic and symmetric (serialize → deserialize → same value).
- Throw informative exceptions for invalid input.
- Register once at startup to avoid repeated configuration cost.


