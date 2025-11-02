## Post-Processing

The `@PostProcess` annotation runs custom logic automatically after a configuration object is loaded. Use it for validation, computing derived fields, or initializing state.

### Annotation

```java
import me.whereareiam.configura.annotation.PostProcess;
```

### Basic usage

```java
import lombok.Data;

@Data
public class Settings {
    private int level;
    private boolean enabled;
    
    public transient int computedValue;
    
    @PostProcess
    public void afterLoad() {
        computedValue = level * 10;
    }
}
```

When you load this config, `afterLoad()` is called automatically:

```java
Settings settings = Config.load("config/settings", Settings.class);
// afterLoad() has been called, computedValue is now 50 (if level was 5)
```

### Validation

```java
@Data
public class ServerConfig {
    private int port;
    private String host;
    
    @PostProcess
    public void validate() {
        if (port < 0 || port > 65535) {
            throw new IllegalStateException("Port must be between 0 and 65535");
        }
        if (host == null || host.isEmpty()) {
            throw new IllegalStateException("Host cannot be empty");
        }
    }
}
```

### One-time initialization

Use `once = true` to run a method only on the first load:

```java
@Data
public class CacheConfig {
    private int size;
    private String strategy;
    
    @PostProcess(once = true)
    public void initializeCache() {
        // Called only once, even if config is reloaded multiple times
        CacheManager.initialize(size, strategy);
    }
    
    @PostProcess
    public void updateSettings() {
        // Called every time
        CacheManager.updateSize(size);
    }
}
```

### Multiple methods

You can have multiple `@PostProcess` methods:

```java
@Data
public class AppConfig {
    private String environment;
    private int logLevel;
    
    @PostProcess
    public void validate() {
        if (environment == null) throw new IllegalStateException("Environment required");
    }
    
    @PostProcess
    public void setupLogging() {
        Logger.setLevel(logLevel);
    }
}
```

### Computed fields

```java
@Data
public class UserConfig {
    private String firstName;
    private String lastName;
    
    public transient String fullName;
    
    @PostProcess
    public void computeDerived() {
        fullName = firstName + " " + lastName;
    }
}
```

### Inheritance

Post-process methods from parent classes are also executed:

```java
public class BaseConfig {
    @PostProcess
    public void baseInit() {
        System.out.println("Base initialized");
    }
}

public class ExtendedConfig extends BaseConfig {
    @PostProcess
    public void extendedInit() {
        System.out.println("Extended initialized");
    }
}
// Both methods are called when loading ExtendedConfig
```

### Tips

- Keep methods lightweight; they run on every load/reload
- Use `transient` for computed fields to prevent serialization
- Throw exceptions early for validation (fail fast)
- Use `once = true` for expensive one-time initialization
- Don't depend on execution order of multiple methods
