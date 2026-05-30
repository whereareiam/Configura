## Post-Processing

The post-process feature runs methods after a configuration object has been loaded.

### Wiring

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.postprocess.PostProcessFeature;

Configura config = Config.builder()
		.feature(PostProcessFeature.defaults())
		.build();
```

### Basic usage

```java
import me.whereareiam.configura.feature.postprocess.api.PostProcess;

public class Settings {
	public int level;
	public transient int computedValue;

	@PostProcess
	public void afterLoad() {
		computedValue = level * 10;
	}
}
```

### One-time processing

```java
public class CacheConfig {
	public int size;

	@PostProcess(once = true)
	public void initializeCache() {
		CacheManager.initialize(size);
	}
}
```

### Validation

```java
public class ServerConfig {
	public int port;

	@PostProcess
	public void validate() {
		if (port < 0 || port > 65535)
			throw new IllegalStateException("Port must be between 0 and 65535");
	}
}
```
