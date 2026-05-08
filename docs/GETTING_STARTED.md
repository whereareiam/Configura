## Getting Started

This guide walks beginners through the basics: prerequisites, adding the dependency, defining a model with Lombok,
templating defaults, and reading/writing configs with YAML by default.

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
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.annotation.template.type.Literal;

@Data
public class AppConfig {
	@Template(literal = @Literal(text = "world"))
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

### Customize serialization with a Jackson module

Configura now uses Jackson modules directly for custom serialization behavior.

```java
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

SimpleModule module = new SimpleModule("duration-module", Version.unknownVersion());
module.addSerializer(MyType.class, new MyTypeSerializer());
module.addDeserializer(MyType.class, new MyTypeDeserializer());

Config config = Config.builder()
		.module(module)
		.build();
```

### Control merge behavior with @Merge

By default, templates use the `DEEP_DEFAULTS` built-in merge preset. You can select a different preset per field, set a global default, or register named custom policies.

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.merge.MergePolicy;
import lombok.Data;
import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.type.MergePreset;

@Data
public class CommandsConfig {
    @Merge(preset = MergePreset.SOURCE_OWNS_FIELD)
    private Map<String, CommandDefinition> commands;
}
```

**Built-in presets:**
- `DEEP_DEFAULTS`: Recursively fill missing values from templates
- `SOURCE_OWNS_FIELD`: If the source provides a value, the source owns the field
- `SOURCE_OWNS_MAP`: Source map entries are preserved as-is
- `SOURCE_OWNS_LIST`: Source list is preserved as-is
- `NEVER_TEMPLATE`: Do not apply template values
- `DECLARED_KEYS_ONLY_MAP`: Only merge into map keys already declared by the source

```java
Config config = Config.builder()
        .defaultMergePreset(MergePreset.SOURCE_OWNS_FIELD)
        .mergePolicy("declaredKeysOnly", MergePolicy.builder()
                .mapMode(MergePolicy.MapMode.DECLARED_SOURCE_KEYS_ONLY)
                .build())
        .build();
```

Explicit source `null` is preserved during merge/update by default.

See [TEMPLATING.md](TEMPLATING.md#merge-policies-controlling-template-behavior) for detailed examples.

### Read or create with defaults

Build a configured `Config` engine. YAML is used by default. Templates are applied on save/update.

```java
import me.whereareiam.configura.Config;

Config config = Config.builder().build();
AppConfig appConfig = new AppConfig();
config.objects().save("app-config", appConfig);
appConfig = config.objects().read("app-config", AppConfig.class);

System.out.println("Hello, " + appConfig.getName() + "!");
```

### Alternatives: explicit read/write

You can also keep static helpers for one-off use, or use a built `Config` for an explicit format. Omit extensions; the
configured format determines the output.

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.type.Format;

AppConfig cfg = new AppConfig();
cfg.setName("world");

Config.save("app-config", cfg);
AppConfig fromYaml = Config.load("app-config", AppConfig.class);

Config json = Config.builder().format(Format.JSON).build();
json.objects().write("app-config", cfg);
AppConfig fromJson = json.objects().read("app-config", AppConfig.class);
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

### Templating examples

Templates let you declare default values for simple values, lists, and object-like maps.

<details>
  <summary>Simple value default</summary>

  ```java
  import lombok.Data;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.annotation.template.type.Literal;

@Data
public class GreetingConfig {
	@Template(literal = @Literal(text = "world"))
	private String name;
}
  ```

</details>

<details>
  <summary>List default</summary>

  ```java
  import lombok.Data;

import java.util.List;

import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.annotation.template.type.Literal;

@Data
public class RolesConfig {
	@Template(items = {@Literal(text = "user"), @Literal(text = "admin")})
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

import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.annotation.template.type.Property;

@Data
public class DbConfig {
	@Template(properties = {
			@Property(name = "host", value = @Literal(text = "localhost")),
			@Property(name = "port", value = @Literal(number = "5432"))
	})
	private Map<String, Object> defaults;
}
  ```

</details>
