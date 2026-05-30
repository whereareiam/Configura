## Configura

Lightweight, model-driven configuration framework for Java with:
- core document loading, saving, merging, defaults, and migration
- optional feature modules such as extensions, polymorphism, and post-processing
- YAML and JSON support out of the box
- a public adapter SPI for teaching Configura how custom types merge

### Documentation

- [Getting Started](docs/GETTING_STARTED.md)

Core:
- [Merge Overview](docs/MERGE.md)
- [Strategies](docs/merge/STRATEGIES.md)
- [Defaults Resolution](docs/merge/DEFAULTS.md)
- [Type Adapters](docs/merge/TYPE_ADAPTERS.md)
- [Annotation DSL](docs/merge/ANNOTATIONS.md)
- [Migration](docs/MIGRATION.md)

Features:
- [Feature Overview](docs/FEATURES.md)
- [Extensions](docs/features/EXTENSIONS.md)
- [Polymorphic Models](docs/features/POLYMORPHIC.md)
- [Post-Processing](docs/features/POST_PROCESSING.md)

### Installation

<a href="https://github.com/whereareiam/Configura/releases">
    <img src="https://maven.whereareiam.me/api/badge/latest/release/me/whereareiam/configura?color=40c14a&name=Latest release" />
</a>
<a href="https://github.com/whereareiam/Configura/actions/workflows/publish-dev.yml">
    <img src="https://maven.whereareiam.me/api/badge/latest/development/me/whereareiam/configura?color=c15340&name=Latest dev" />
</a>

Artifacts are published to:
- `https://maven.whereareiam.me/release`
- `https://maven.whereareiam.me/development`

Base dependency:

```kotlin
repositories {
    maven("https://maven.whereareiam.me/release")
    maven("https://maven.whereareiam.me/development")
}

dependencies {
    implementation("me.whereareiam:configura:0.0.1")
}
```

Optional features are separate dependencies:

```kotlin
dependencies {
    implementation("me.whereareiam:configura:0.0.1")

    implementation("me.whereareiam.configura.feature:extension:0.0.1")
    implementation("me.whereareiam.configura.feature:polymorphic:0.0.1")
    implementation("me.whereareiam.configura.feature:postprocess:0.0.1")
}
```

### Quick Start

Define a core-only model:

```java
import me.whereareiam.configura.annotation.merge.MergeValue;

public class HelloConfig {
	@MergeValue(text = "world")
	public String name;
}
```

Load and persist it:

```java
import me.whereareiam.configura.Config;

HelloConfig config = Config.update("config/hello", HelloConfig.class);
```

`update(...)` creates the file when missing, fills missing values from defaults, writes the merged result, and returns the bound model.

If you want your own reusable setup:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.type.Format;

Configura yaml = Config.builder()
		.format(Format.YAML)
		.build();

HelloConfig config = yaml.update("config/hello", HelloConfig.class);
```

Optional features are installed explicitly:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.postprocess.PostProcessFeature;

Configura yaml = Config.builder()
		.feature(PostProcessFeature.defaults())
		.build();
```

See [Getting Started](docs/GETTING_STARTED.md) for a full walkthrough.

### Core Model

- Merge behavior is selected through field strategies such as `@Merge(SourceOwnsField.class)`.
- Shapes such as objects, maps, and lists are core merge concepts.
- Defaults supply seed data for those shapes through the merge annotation DSL or through registered defaults resolvers/providers.
- Custom type families are taught through `MergeTypeAdapter`.
