## Configura

Lightweight, model-driven configuration framework for Java with merge defaults, zero-boilerplate load/save
helpers, and pluggable formats (YAML/JSON).

### Table of contents

- [Getting Started](docs/GETTING_STARTED.md)
- [Versioned Config Migrations](docs/VERSIONING.md)
- [Merge defaults](docs/MERGE_DEFAULTS.md)
- [Post-processing](docs/POST_PROCESSING.md)
- [Polymorphic models](docs/POLYMORPHIC.md)

### Features

- **Model‑driven**: Define plain Java classes as your config model; no frameworks required.
- **Merge defaults**: Use `@Defaults` to declare defaults for scalars, lists, and object-like maps.
- **Versioned migrations**: Register class-per-step migrations with `ConfigDocument` or `@SchemaVersion` support.
- **Post-processing**: Use `@PostProcess` to run validation, compute derived fields, or initialize state after loading.
- **Multiple formats**: YAML and JSON supported out of the box.
- **Readable durations**: `java.time.Duration` fields use config-friendly strings such as `10m` or `2h30m`.
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

1) Define a simple model with inline merge defaults:

```java
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.annotation.PostProcess;

public class HelloConfig {
	@Defaults(text = "world")
	public String name;
	
	@PostProcess
	public void afterLoad() {
		System.out.println("Config loaded!");
	}
}
```

2) Use the static `Config` facade directly, or build a configured `Configura` instance when you want your own reusable setup:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;

// Uses the active configured helper
HelloConfig cfg = Config.update("config/hello", HelloConfig.class);
// afterLoad() has been called automatically

// Or build your own configured instance
Configura yaml = Config.builder().build();
HelloConfig other = yaml.update("config/hello-other", HelloConfig.class);
```

`Config.configure(...)` changes the active configured helper used by static `Config.*(...)` methods.
`Config.defaults()` returns the bootstrap default `Configura` instance.

3) Want object‑like defaults? Use `@Defaults(properties=...)`:

```java
import me.whereareiam.configura.annotation.Defaults;

import java.util.Map;

public class DbConfig {
	@Defaults(properties = {
			@Defaults.Property(name = "host", text = "localhost"),
			@Defaults.Property(name = "port", number = "5432")
	})
	public Map<String, Object> defaults;
}
```

See [Getting Started](docs/GETTING_STARTED.md) for more details and examples.
