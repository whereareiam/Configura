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

Distributed via JitPack.

<details>
  <summary>Add dependency (Gradle)</summary>

```kotlin
repositories {
    maven(url = uri("https://jitpack.io"))
}

dependencies {
    // Use a release tag (e.g. 1.0.0) or a branch snapshot (e.g. dev-SNAPSHOT)
    implementation("com.github.whereareiam:Configura:dev-SNAPSHOT")
}
  ```

</details>

<details>
  <summary>Add dependency (Maven)</summary>

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- Use a release tag (e.g. 1.0.0) or a branch snapshot (e.g. dev-SNAPSHOT) -->
    <dependency>
        <groupId>com.github.whereareiam</groupId>
        <artifactId>Configura</artifactId>
        <version>dev-SNAPSHOT</version>
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
cfg = Config.updateRead("config/hello", cfg);

// Use it in your code
System.out.println("Hello, " + cfg.name + "!");
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

See [Getting Started](docs/GETTING_STARTED.md) for more details and examples.


