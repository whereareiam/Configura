## Getting Started

This guide introduces the current Configura model:
- core dependency for loading, saving, defaults, merging, migration, and custom type adapters
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
import me.whereareiam.configura.annotation.merge.MergeValue;

public class AppConfig {
	@MergeValue(text = "world")
	public String name;
}
```

### Read and write

Using the static facade:

```java
import me.whereareiam.configura.Config;

AppConfig config = Config.update("config/app", AppConfig.class);
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

### Teach a custom type

Use a type adapter when a declared type needs custom merge behavior:

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;

public final class Envelope<T> {
	public T value;
}

public final class EnvelopeTypeAdapter implements MergeTypeAdapter {
	@Override
	public boolean supports(MergeTypeDescriptor descriptor, MergePolicy policy) {
		return descriptor.getDeclaredType() == Envelope.class;
	}

	@Override
	public Class<?> resolveChildType(MergeTypeDescriptor descriptor, MergePolicy policy) {
		Class<?> generic = descriptor.resolveGenericArgument(0);
		return generic != null ? generic : descriptor.getDeclaredType();
	}

	@Override
	public JsonNode merge(MergeTypeAdapterContext context) {
		JsonNode source = context.objectMember(context.getSourceNode(), "value");
		JsonNode defaults = context.objectMember(context.getDefaultNode(), "value");

		ObjectNode result = context.getMapper().createObjectNode();
		result.set("value", context.mergeChildren(source, defaults));
		return result;
	}
}

Configura yaml = Config.builder()
		.mergeTypeAdapter(new EnvelopeTypeAdapter())
		.build();
```

### Use the merge/defaults DSL

Configura’s built-in annotations are grouped under `me.whereareiam.configura.annotation.merge`.

```java
import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.annotation.merge.MergeList;
import me.whereareiam.configura.annotation.merge.MergeObject;
import me.whereareiam.configura.annotation.merge.MergeValue;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;
import me.whereareiam.configura.type.merge.tree.list.ListMode;

import java.util.List;

public class ProvidersConfig {
	@Merge(SourceOwnsField.class)
	@MergeValue(text = "localhost")
	public String host;

	@MergeObject(properties = {
			@MergeObject.Property(name = "attempts", number = "3")
	})
	public RetryConfig retry;

	@MergeList(mode = ListMode.KEYED, key = "id")
	public List<ProviderEntry> providers;
}
```

### Register migrations

```java
Configura yaml = Config.builder()
		.versioned(AppConfig.class, spec -> spec.currentVersion(1))
		.build();
```

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

- [Merge Overview](MERGE.md)
- [Strategies](merge/STRATEGIES.md)
- [Defaults Resolution](merge/DEFAULTS.md)
- [Type Adapters](merge/TYPE_ADAPTERS.md)
- [Annotation DSL](merge/ANNOTATIONS.md)
