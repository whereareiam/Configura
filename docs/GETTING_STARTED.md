## Getting Started

This guide introduces the current Configura model:
- core dependency for loading, saving, defaults and merging
- optional feature dependencies for extensions, polymorphism, and post-processing

### Prerequisites

- Java 17
- Gradle or Maven

### Base dependency

```kotlin
repositories {
    maven("https://maven.whereareiam.me/release")
    maven("https://maven.whereareiam.me/development")
}

dependencies {
    implementation("me.whereareiam:configura:0.0.1")
}
```

### Optional feature dependencies

Add these only when you use the feature:

```kotlin
dependencies {
    implementation("me.whereareiam.configura.feature:extension:0.0.1")
    implementation("me.whereareiam.configura.feature:polymorphic:0.0.1")
    implementation("me.whereareiam.configura.feature:postprocess:0.0.1")
}
```

### Define a core model

```java
public class AppConfig {
	public String name = "world";
}
```

### Read and write

Using a plain instance:

```java
import me.whereareiam.configura.Config;

AppConfig config = Config.yaml().update("config/app", AppConfig.class);
```

Using your own configured instance:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.type.Format;

Configura yaml = Config.builder()
		.format(Format.YAML)
		.build();

AppConfig config = yaml.update("config/app", AppConfig.class);
```

### Add defaults providers

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;

public final class AppDefaults implements DefaultsProvider<AppConfig> {
	@Override
	public AppConfig supply(AppConfig config) {
		config.name = "service";
		return config;
	}
}

Configura yaml = Config.builder()
		.defaults(AppDefaults.class)
		.build();
```

### Decide how a field is merged

```java
import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.type.merge.MissingEntries;
import me.whereareiam.configura.type.merge.WhenAbsent;

public class AppConfig {
	public String host = "localhost";

	@Merge(absent = WhenAbsent.LEAVE_ABSENT)
	public RetryConfig retry;

	@Merge(key = "id", missingEntries = MissingEntries.OMIT)
	public List<ProviderEntry> providers;
}
```

See [Defaults and merging](MERGE.md) for every option.

### Install optional features

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.postprocess.PostProcessFeature;

Configura yaml = Config.builder()
		.feature(PostProcessFeature.defaults())
		.build();
```

### Continue reading

- [Defaults and merging](MERGE.md)
- [Features](FEATURES.md)
