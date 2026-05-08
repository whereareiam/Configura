## Configura

Lightweight, model‑driven configuration framework for Java with first‑class templating, zero‑boilerplate load/save
helpers, and pluggable formats (YAML/JSON).

### Table of contents

- [Getting Started](docs/GETTING_STARTED.md)
- [Templating guide](docs/TEMPLATING.md)
- [Post-processing](docs/POST_PROCESSING.md)
- [Polymorphic models](docs/POLYMORPHIC.md)

### Features

- **Model‑driven**: Define plain Java classes as your config model; no frameworks required.
- **Inline templating**: Use `@Template` to declare defaults for scalars, lists, and object‑like maps.
- **Post-processing**: Use `@PostProcess` to run validation, compute derived fields, or initialize state after loading.
- **Multiple formats**: YAML and JSON supported out of the box.
- **Jackson-native & extensible**: Pluggable readers/writers and custom Jackson modules.

### Installation

<a href="https://github.com/whereareiam/Configura/releases">
    <img src="https://maven.whereareiam.me/api/badge/latest/release/me/whereareiam/configura?color=40c14a&name=Latest dev" />
</a>
<a href="https://github.com/whereareiam/Configura/actions/workflows/publish-dev.yml">
    <img src="https://maven.whereareiam.me/api/badge/latest/development/me/whereareiam/configura?color=c15340&name=Latest dev" />
</a>

Artifacts are published to our repository. Use the release realm for stable versions and the development realm for dev
builds (`dev` or `dev-<HASH>`).

<details>
  <summary>Add dependency (Gradle)</summary>

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
}
```

</details>

<details>
  <summary>Add dependency (Maven)</summary>

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
    <!-- If your Maven requires, enable releases/snapshots flags accordingly -->
</repositories>

<dependencies>
<!-- Release example -->
<dependency>
    <groupId>me.whereareiam</groupId>
    <artifactId>configura</artifactId>
    <version>0.0.1</version>
</dependency>

<!-- Development example (optional) -->
<!-- <dependency>
  <groupId>me.whereareiam</groupId>
  <artifactId>configura</artifactId>
  <version>dev</version>
</dependency> -->
</dependencies>
```

</details>

### Quick start

1) Define a simple model with an inline template (defaults are materialized/merged as needed):

```java
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.annotation.PostProcess;

public class HelloConfig {
	@Template(literal = @Literal(text = "world"))
	public String name;
	
	@PostProcess
	public void afterLoad() {
		System.out.println("Config loaded!");
	}
}
```

2) Build a configured `Config` engine and update without specifying an extension (defaults to YAML):

```java
import me.whereareiam.configura.Config;

Config config = Config.builder().build();
// Writes defaults if needed and then re‑reads from disk
HelloConfig cfg = config.objects().update("config/hello", HelloConfig.class);
// afterLoad() has been called automatically

// Use it in your code
System.out.println("Hello, " + cfg.name + "!");
```

Static helpers like `Config.update(...)` and `Config.save(...)` are still available for one-off use, but the built `Config`
instance is the preferred API when you want a configured engine object.

3) Want object‑like defaults? Use `@Template(properties=...)`:

```java
import me.whereareiam.configura.annotation.Template;
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
