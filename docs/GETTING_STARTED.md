## Getting Started

This guide walks beginners through the basics: prerequisites, adding the dependency, defining a model with Lombok,
merge defaults, and reading/writing configs with YAML by default.

### Prerequisites

- **Java 17** (source/target)
- **Gradle or Maven**

### Install the library

<a href="https://github.com/whereareiam/Configura/releases">
    <img src="https://maven.whereareiam.me/api/badge/latest/release/me/whereareiam/configura?color=40c14a&name=Latest dev" />
</a>
<a href="https://github.com/whereareiam/Configura/actions/workflows/publish-dev.yml">
    <img src="https://maven.whereareiam.me/api/badge/latest/development/me/whereareiam/configura?color=c15340&name=Latest dev" />
</a>

Use our repository as described below or see Installation in the project `README.md`.

<details>
  <summary>Gradle (Kotlin DSL)</summary>

  ```kotlin
  repositories {
    // release builds
    maven("https://maven.whereareiam.me/release")
    // development builds (optional)
    maven("https://maven.whereareiam.me/development")
}

dependencies {
    // Release example
    implementation("me.whereareiam:configura:0.0.1")

    // Development example (optional)
    // implementation("me.whereareiam:configura:dev")
    // implementation("me.whereareiam:configura:dev-<GIT_HASH>")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
}
  ```

</details>

<details>
  <summary>Maven</summary>

  ```xml

<repositories>
    <!-- release builds -->
    <repository>
        <id>release</id>
        <url>https://maven.whereareiam.me/release</url>
    </repository>
    <!-- development builds (optional) -->
    <repository>
        <id>development</id>
        <url>https://maven.whereareiam.me/development</url>
    </repository>
</repositories>

<dependencies>
<dependency>
    <groupId>me.whereareiam</groupId>
    <artifactId>configura</artifactId>
    <version>0.0.1</version>
</dependency>
<!-- Development example (optional)
<dependency>
  <groupId>me.whereareiam</groupId>
  <artifactId>configura</artifactId>
  <version>dev</version>
</dependency>
-->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.32</version>
    <scope>provided</scope>
</dependency>
</dependencies>
<build>
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
            <source>17</source>
            <target>17</target>
            <annotationProcessorPaths>
                <path>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <version>1.18.32</version>
                </path>
            </annotationProcessorPaths>
        </configuration>
    </plugin>
</plugins>
</build>
  ```

</details>

### Define a model (YAML-first, Lombok)

We recommend a simple POJO model using Lombok for brevity.

```java
import lombok.Data;
import me.whereareiam.configura.annotation.Defaults;

@Data
public class AppConfig {
	@Defaults(text = "world")
	private String name;
}
```

### Rename fields with Jackson annotations

Use `@JsonProperty("...")` to map a Java field to a different serialized key name.

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ServerConfig {
	@JsonProperty("bind_port")
	private int port = 8080;

	@JsonProperty("bind_host")
	private String host = "127.0.0.1";
}
```

This yields YAML like:

```yaml
bind_port: 8080
bind_host: 127.0.0.1
```

### Use readable durations

`java.time.Duration` fields are written as compact config strings by default.

```java
import java.time.Duration;
import lombok.Data;

@Data
public class SessionConfig {
	private Duration defaultTtl = Duration.ofHours(2).plusMinutes(30);
}
```

This yields YAML like:

```yaml
defaultTtl: "2h30m"
```

Configura also reads values such as `10m`, `1h30m`, `5s`, `250ms`, ISO values like `PT10M`, and bare numbers as minutes.

### Customize serialization with a Jackson module

Configura now uses Jackson modules directly for custom serialization behavior.

```java
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

SimpleModule module = new SimpleModule("duration-module", Version.unknownVersion());
module.addSerializer(MyType.class, new MyTypeSerializer());
module.addDeserializer(MyType.class, new MyTypeDeserializer());

Configura config = Config.builder()
		.module(module)
		.build();
```

### Control tree merge behavior

Configura merges a serialized tree. Every property in that tree can participate in merge defaults.

- `@Merge(...)` controls the default-ownership behavior of the property itself
- `@MergeMap(...)` controls how map entries under that property are merged
- `@MergeList(...)` controls how list items under that property are merged

For ordinary object properties, `@Merge(...)` is enough:

```java
import me.whereareiam.configura.merge.annotation.Merge;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;

public class CommandsConfig {
    @Merge(SourceOwnsField.class)
    public Map<String, CommandDefinition> commands;
}
```

Built-in `@Merge(...)` strategies:
- `DeepDefaults`: recursively fill missing values from merge defaults
- `SourceOwnsField`: if the source provides a value, the source owns the whole property
- `NeverDefaults`: do not apply defaults for this property
- `StructuralObject`: keep an object present without deep-filling declared children

For map and list properties, use the tree-specific annotations:

```java
import me.whereareiam.configura.merge.annotation.MergeList;
import me.whereareiam.configura.type.merge.tree.list.ListMode;
import me.whereareiam.configura.type.merge.tree.list.ListPresence;
import me.whereareiam.configura.type.merge.tree.list.ListUnknownEntries;

public class ProvidersConfig {
    @MergeList(
            mode = ListMode.KEYED,
            key = "id",
            presence = ListPresence.DECLARED_ONLY,
            unknownEntries = ListUnknownEntries.ALLOW
    )
    public List<ProviderEntry> providers;
}
```

```java
import me.whereareiam.configura.merge.annotation.MergeMap;
import me.whereareiam.configura.type.merge.tree.map.MapPresence;
import me.whereareiam.configura.type.merge.tree.map.MapUnknownEntries;

public class RoutingConfig {
    @MergeMap(
            presence = MapPresence.DECLARED_ONLY,
            unknownEntries = MapUnknownEntries.ALLOW
    )
    public Map<String, Scenario> scenarios;
}
```

List/map tree behavior:
- `@MergeList(mode = KEYED)`: merge list items by a stable key such as `id`
- `@MergeList(mode = PLAIN)`: seed the list when missing, otherwise keep the declared list
- `@MergeMap`: merge map entries by map key
- `DECLARED_ONLY`: seed when missing and merge only declared entries
- `SEED_DEFAULTS`: also append default-only entries after declared ones
- `DEFAULT_DOMAIN_ONLY`: reject unknown source entries

You can still combine `@Merge(...)` with `@MergeMap(...)` or `@MergeList(...)` when the property should not use the default `DeepDefaults` ownership behavior. For example, `@Merge(SourceOwnsField.class)` makes the declared map/list fully source-owned even if a tree merge annotation is present.

Explicit source `null` is preserved during merge/update by default.

See [MERGE_DEFAULTS.md](MERGE_DEFAULTS.md#merge-strategies) for detailed examples.
When defaults are not enough and you need to rename, move, or restructure fields across releases, use
[VERSIONING.md](VERSIONING.md).

### Read or create with defaults

Build a configured `Configura` instance when you want an explicit reusable setup. YAML is used by default. Merge defaults are applied on save/update.

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;

Configura config = Config.builder().build();
AppConfig appConfig = new AppConfig();
config.save("app-config", appConfig);
appConfig = config.read("app-config", AppConfig.class);

System.out.println("Hello, " + appConfig.getName() + "!");
```

### Alternatives: explicit read/write

You can also use the active static helper via `Config.save(...)` / `Config.read(...)`, or use a built `Configura` for an explicit format.
Omit extensions; the configured format determines the output.

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.type.Format;

AppConfig cfg = new AppConfig();
cfg.setName("world");

Config.save("app-config", cfg);
AppConfig fromYaml = Config.read("app-config", AppConfig.class);

Configura json = Config.builder().format(Format.JSON).build();
json.write("app-config", cfg);
AppConfig fromJson = json.read("app-config", AppConfig.class);
```

### Post-processing with @PostProcess

You can run custom logic after a configuration is loaded using `@PostProcess`:

```java
import me.whereareiam.configura.annotation.PostProcess;

@Data
public class Settings {
    private int level;
    private boolean enabled;
    
    public transient int computedValue; // Not serialized
    
    @PostProcess
    public void afterLoad() {
        // Runs automatically after config is loaded
        computedValue = level * 10;
        
        // Validation example
        if (level < 0) {
            throw new IllegalStateException("Level must be positive");
        }
    }
}
```

### Merge default examples

`@Defaults` declares merge defaults for simple values, lists, and object-like maps.

<details>
  <summary>Simple value default</summary>

  ```java
  import lombok.Data;
import me.whereareiam.configura.annotation.Defaults;

@Data
public class GreetingConfig {
	@Defaults(text = "world")
	private String name;
}
  ```

</details>

<details>
  <summary>List default</summary>

  ```java
  import lombok.Data;

import java.util.List;

import me.whereareiam.configura.annotation.Defaults;

@Data
public class RolesConfig {
	@Defaults(stringItems = {"user", "admin"})
	private List<String> roles;
}
  ```

</details>

<details>
  <summary>Object/map default</summary>

  ```java
  import lombok.Data;

import java.util.List;
import java.util.Map;

import me.whereareiam.configura.annotation.Defaults;

@Data
public class DbConfig {
	@Defaults(properties = {
			@Defaults.Property(name = "host", text = "localhost"),
			@Defaults.Property(name = "port", number = "5432")
	})
	private Map<String, Object> defaults;
}
  ```

</details>
