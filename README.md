## Configura

Lightweight, model‑driven configuration framework for Java with first‑class templating, zero‑boilerplate load/save
helpers, and pluggable formats (YAML/JSON).

### Table of contents

- [Getting Started](docs/GETTING_STARTED.md)
- [Templating guide](docs/TEMPLATING.md)
- [Type adapters](docs/TYPE_ADAPTERS.md)

### Features

- **Model‑driven**: Define plain Java classes as your config model; no frameworks required.
- **Inline templating**: Use `@Template` to declare defaults for scalars, lists, and object‑like maps.
- **Multiple formats**: YAML and JSON supported out of the box.
- **Service‑based & extensible**: Pluggable readers/writers, global type adapters, and service discovery.

### Installation

Currently distributed via local Maven.

<details>
  <summary>Publish to local Maven (one‑time)</summary>

  ```bash
  # 1) Clone the repository
  git clone https://github.com/whereareiam/Configura.git
  cd Configura

  # 2) Publish artifacts to your local Maven repository
  ./gradlew publishToMavenLocal   # on Linux/macOS
  # or
  gradlew.bat publishToMavenLocal # on Windows
  ```

</details>

<details>
  <summary>Add dependency (Gradle)</summary>

  ```kotlin
  repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("me.whereareiam:configura:dev")
}
  ```

</details>

<details>
  <summary>Add dependency (Maven)</summary>

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
</dependencies>
  ```

</details>

### Quick start

1) Define a simple model with an inline template (defaults are materialized/merged as needed):

```java
import me.whereareiam.configura.annotation.template.Template;
import me.whereareiam.configura.annotation.template.type.Literal;

public class HelloConfig {
	@Template(literal = @Literal(text = "world"))
	public String name;
}
```

2) Read/update using the static helper without specifying an extension (defaults to YAML):

```java
import me.whereareiam.configura.Config;

HelloConfig cfg = new HelloConfig();
// Writes defaults if needed and then re‑reads from disk
cfg =Config.

updateRead("config/hello",cfg);

// Use it in your code
System.out.

println("Hello, "+cfg.name +"!");
```

3) Want object‑like defaults? Use `@Template(properties=...)`:

```java
import me.whereareiam.configura.annotation.template.Template;
import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.annotation.template.type.Property;

import java.util.Map;

public class DbConfig {
	@Template(properties = {
			@Property(name = "host", value = @Literal(text = "localhost")),
			@Property(name = "port", value = @Literal(number = "5432"))
	})
	public Map<String, Object> defaults;
}
```

See `docs/GETTING_STARTED.md` for more details and examples.


