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

### Map fields to custom keys with @Field

Use `@Field(name = "...")` to map a Java field to a nested key path in the file.

```java
import lombok.Data;
import me.whereareiam.configura.annotation.Field;

@Data
public class ServerConfig {
	@Field(name = "server.port")
	private int port = 8080;

	@Field(name = "server.host")
	private String host = "127.0.0.1";
}
```

This yields YAML like:

```yaml
server:
  port: 8080
  host: 127.0.0.1
```

### Control merge behavior with @Policy

By default, `updateRead` merges defaults and prunes unknown fields. You can change this at the class level or per field.

```java
import lombok.Data;
import me.whereareiam.configura.annotation.Policy;

@Data
@Policy(mergeOnUpdate = false) // class-level: never rewrite on updateRead
public class NoRewriteConfig {
	private String note;
}
```

With class-level `mergeOnUpdate = false`, `Config.update("file", cfg)` will not rewrite the file; it will load
as‑is.

Per-field override example:

```java
import lombok.Data;
import me.whereareiam.configura.annotation.Policy;

@Data
public class MixedPolicyConfig {
	private String safeToMerge;

	@Policy(mergeOnUpdate = false) // this field opts out of merge-driven rewrites
	private String keepAsIs;
}
```

If any field sets `@Policy(mergeOnUpdate = false)`, the update step will skip rewriting to preserve that field.

### Read or create with defaults

Use the static helper. YAML is used by default. Templates are applied on save.

```java
import me.whereareiam.configura.Config;

AppConfig config = new AppConfig();
Config.save("app-config", config);
config = Config.read("app-config", AppConfig.class);

System.out.println("Hello, " + config.getName() + "!");
```

### Alternatives: explicit read/write

You can also read and write explicitly. Omit extensions; the configured format determines the output.

```java
import me.whereareiam.configura.Config;

AppConfig cfg = new AppConfig();
cfg.setName("world");

Config.save("app-config", cfg);
AppConfig fromYaml = Config.read("app-config", AppConfig.class);

Config.writer(Format.JSON).write("app-config", cfg);
AppConfig fromJson = Config.reader(Format.JSON).read("app-config", AppConfig.class);
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

