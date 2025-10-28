## Getting Started

This guide walks beginners through the basics: prerequisites, adding the dependency, defining a model with Lombok, templating defaults, and reading/writing configs with YAML by default.

### Prerequisites
- **Java 17** (source/target)
- **Gradle or Maven**

### Install the library
See Installation in the project `README.md`. In short, publish locally and add the dependency.

<details>
  <summary>Gradle (Kotlin DSL)</summary>

  
  ```kotlin
  repositories {
      mavenLocal()
      mavenCentral()
  }

  dependencies {
      implementation("me.whereareiam:configura:dev")
      compileOnly("org.projectlombok:lombok:1.18.32")
      annotationProcessor("org.projectlombok:lombok:1.18.32")
  }
  ```

</details>

<details>
  <summary>Maven</summary>

  
  ```xml
  <repositories>
    <repository>
      <id>local-maven</id>
      <url>file://${user.home}/.m2/repository</url>
    </repository>
    <repository>
      <id>central</id>
      <url>https://repo1.maven.org/maven2/</url>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>me.whereareiam</groupId>
      <artifactId>configura</artifactId>
      <version>dev</version>
    </dependency>
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
import me.whereareiam.configura.annotation.template.Template;
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

With class-level `mergeOnUpdate = false`, `Config.updateRead("file", cfg)` will not rewrite the file; it will load as‑is.

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
Use the static helper. If the file does not exist, defaults from templates help seed the file when using update-and-read. YAML is used by default when no extension is provided.

```java
import me.whereareiam.configura.Config;

AppConfig config = new AppConfig();
config = Config.updateRead("app-config", config); // no extension → YAML

System.out.println("Hello, " + config.getName() + "!");
```

### Alternatives: explicit load/save
You can also save and load explicitly. Use `.yml`/`.yaml` or `.json` to control the format.

```java
import me.whereareiam.configura.Config;

AppConfig cfg = new AppConfig();
cfg.setName("world");

// YAML
Config.save("app-config.yaml", cfg);
AppConfig fromYaml = Config.load("app-config.yaml", AppConfig.class);

// JSON
Config.save("app-config.json", cfg);
AppConfig fromJson = Config.load("app-config.json", AppConfig.class);
```

### Templating examples
Templates let you declare default values for simple values, lists, and object-like maps.

<details>
  <summary>Simple value default</summary>

  
  ```java
  import lombok.Data;
  import me.whereareiam.configura.annotation.template.Template;
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
  import me.whereareiam.configura.annotation.template.Template;
  import me.whereareiam.configura.annotation.template.type.Literal;

  @Data
  public class RolesConfig {
      @Template(items = { @Literal(text = "user"), @Literal(text = "admin") })
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
  import me.whereareiam.configura.annotation.template.Template;
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

