## Merge Defaults

Merge defaults let you describe what a config should look like when values are missing.

Configura does this in two steps:

1. It builds a defaults tree from `@Defaults` declarations or provider classes.
2. It merges that defaults tree with config data using the selected merge strategy.

That means defaults are not a separate runtime feature. They are input to merge.

### When merge defaults run

Merge defaults are used by these `Config` methods:

- `save(...)`
- `merge(...)`
- `update(...)`

Merge defaults are not used by:

- `read(...)`
- `readNode(...)`
- `readResolvedNode(...)`

Use `read(...)` when you want strict deserialization of what already exists.
Use `save(...)`, `merge(...)`, or `update(...)` when you want missing values filled from defaults.

### Smallest useful example

Start with a single field default:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.Defaults;

public class GreetingConfig {
	@Defaults(text = "world")
	public String name;
}

GreetingConfig config = Config.update("greeting", GreetingConfig.class);
```

Most code can use the simpler static facade:

```java
GreetingConfig config = Config.update("greeting", GreetingConfig.class);
```

Use `Config.defaults().update(...)` only when you explicitly want the bootstrap default configuration instead of the currently configured static helper.

If `greeting.yml` does not exist yet, `update(...)` creates it and persists:

```yaml
name: world
```

If the file already exists and contains:

```yaml
name: Alice
```

then `update(...)` keeps `Alice`, because the value is already present.

### Inline defaults

Inline defaults are the simplest way to declare missing values directly on the model.

Scalar defaults:

```java
import me.whereareiam.configura.annotation.Defaults;

public class AppConfig {
	@Defaults(text = "localhost")
	public String host;

	@Defaults(number = "8080")
	public int port;

	@Defaults(bool = true)
	public boolean enabled;
}
```

List defaults:

```java
import me.whereareiam.configura.annotation.Defaults;

import java.util.List;

public class RolesConfig {
	@Defaults(stringItems = {"user", "admin"})
	public List<String> roles;
}
```

Object-like defaults:

```java
import me.whereareiam.configura.annotation.Defaults;

import java.util.Map;

public class DatabaseConfig {
	@Defaults(properties = {
			@Defaults.Property(name = "host", text = "localhost"),
			@Defaults.Property(name = "port", number = "5432")
	})
	public Map<String, Object> connection;
}
```

These inline declarations are great when the defaults are small and local to one field.

### Provider defaults

Use a provider when defaults are easier to express in Java code or when you want to reuse them.

```java
import me.whereareiam.configura.merge.defaults.MergeDefaultsProvider;

public class AppConfig {
	public String name;
	public RetryConfig retry;

	public static class RetryConfig {
		public int attempts;
	}
}

public final class AppDefaults implements MergeDefaultsProvider<AppConfig> {
	@Override
	public AppConfig supply(AppConfig config) {
		config.name = "service";
		config.retry = new AppConfig.RetryConfig();
		config.retry.attempts = 3;
		return config;
	}
}
```

You can use a provider in two ways.

Programmatic registration:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;

Configura config = Config.builder()
		.defaults(AppDefaults.class)
		.build();
```

Model-declared provider:

```java
import me.whereareiam.configura.annotation.Defaults;

@Defaults(provider = @Defaults.Provider(AppDefaults.class))
public class AppConfig {
	public String name;
	public RetryConfig retry;
}
```

Use inline defaults when the value is simple.
Use a provider when the default structure is bigger, computed, or reused.

### Choosing `save`, `merge`, or `update`

These three methods all use merge defaults, but they serve different jobs.

`update(path, Type.class)`
- Reads existing file content.
- Creates an empty model.
- Fills missing values from defaults.
- Persists the merged result.
- Returns the updated model.

Use it when you want to maintain a config file on disk.

```java
AppConfig config = Config.update("app", AppConfig.class);
```

`merge(path, value)`
- Reads existing file content.
- Merges that content with defaults derived from the model you pass in.
- Does not write anything back.
- Returns the merged model.

Use it when you want to preview or compute the result without persisting.

```java
AppConfig merged = Config.merge("app", new AppConfig());
```

`save(path, value)`
- Takes the model you pass in as the source of truth.
- Applies defaults and merge rules to that model.
- Persists the result.

Use it when you already have the model instance you want to write.

```java
AppConfig config = Config.update("app", AppConfig.class);
config.name = "custom-service";
Config.save("app", config);
```

### Tree behavior for objects, maps, and lists

Merge defaults are applied across one serialized tree.

For plain object properties, the default behavior is recursive: missing nested fields are filled.

```java
import me.whereareiam.configura.annotation.Defaults;

public class ServiceConfig {
	@Defaults(text = "svc")
	public String name;

	@Defaults(object = @Defaults.Object(properties = {
			@Defaults.Property(name = "retries", number = "3")
	}))
	public RetryConfig retry;

	public static class RetryConfig {
		public int retries;
	}
}
```

For maps and lists, Configura needs extra rules because it must decide what to do with entries or items, not just fields.

Map example:

```java
import me.whereareiam.configura.annotation.Defaults;
import me.whereareiam.configura.merge.annotation.MergeMap;
import me.whereareiam.configura.type.merge.tree.map.MapPresence;
import me.whereareiam.configura.type.merge.tree.map.MapUnknownEntries;

import java.util.Map;

public class RoutingConfig {
	@Defaults(properties = {
			@Defaults.Property(name = "authentication", text = "auth"),
			@Defaults.Property(name = "registration", text = "register")
	})
	@MergeMap(
			presence = MapPresence.DECLARED_ONLY,
			unknownEntries = MapUnknownEntries.ALLOW
	)
	public Map<String, String> scenarios;
}
```

List example:

```java
import me.whereareiam.configura.merge.annotation.MergeList;
import me.whereareiam.configura.type.merge.tree.list.ListMode;
import me.whereareiam.configura.type.merge.tree.list.ListPresence;
import me.whereareiam.configura.type.merge.tree.list.ListUnknownEntries;

import java.util.List;

public class ProvidersConfig {
	@MergeList(
			mode = ListMode.KEYED,
			key = "id",
			presence = ListPresence.SEED_DEFAULTS,
			unknownEntries = ListUnknownEntries.ALLOW
	)
	public List<ProviderEntry> providers;

	public static class ProviderEntry {
		public String id;
		public String endpoint;
	}
}
```

Useful mental model:

- objects merge by field name
- maps merge by map key
- keyed lists merge by item key

### Strategy annotations

`@Merge(...)` controls property-level ownership behavior.

Built-in property strategies:

- `DeepDefaults`: recursively fill missing values from defaults
- `SourceOwnsField`: if the source provides a value, the source owns the whole property
- `NeverDefaults`: do not apply defaults for this property
- `StructuralObject`: keep an object present without deep-filling its declared children

Example:

```java
import me.whereareiam.configura.merge.annotation.Merge;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;

import java.util.Map;

public class CommandsConfig {
	@Merge(SourceOwnsField.class)
	public Map<String, CommandConfig> commands;
}
```

`@MergeMap(...)` controls map-entry behavior under a map field.

- `presence = DECLARED_ONLY` keeps omitted default entries omitted
- `presence = SEED_DEFAULTS` adds omitted default entries back
- `presence = DEFAULT_DOMAIN_ONLY` rejects unknown source entries

`@MergeList(...)` controls list-item behavior under a list field.

- `mode = PLAIN` treats the list as a whole value
- `mode = KEYED` merges matching items by key
- `presence` controls whether omitted default items are restored
- `unknownEntries` controls what to do with extra source items

`@Merge(...)` still matters on map/list fields. It controls whether the whole property is mergeable at all before the map/list-specific rules apply.

### Advanced customization

`Config.builder()` already registers the built-in property plugins and the built-in annotation-backed policy resolver, so `@Merge(...)`, `@MergeMap(...)`, and `@MergeList(...)` work out of the box.

If you need custom behavior, you can extend the merge runtime.

Custom property plugin:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.merge.plugin.MergePlugin;

MergePlugin plugin = ...;

Configura config = Config.builder()
		.mergePlugin(plugin)
		.build();
```

Custom property policy resolver:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.merge.policy.MergePolicyResolver;

MergePolicyResolver resolver = ...;

Configura config = Config.builder()
		.policyResolver(resolver)
		.build();
```

Named custom strategy:

```java
import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.merge.annotation.Merge;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;

public final class AlwaysDefault implements FieldMergeStrategy {
	@Override
	public JsonNode merge(FieldMergeContext context) {
		return context.getDefaultNode();
	}
}

Configura config = Config.builder()
		.mergeStrategy("alwaysDefault", AlwaysDefault.class)
		.build();

public class FeatureConfig {
	@Merge(named = "alwaysDefault")
	public String mode;
}
```

In practice, most projects only need:

- inline defaults for small values
- providers for bigger defaults
- `update(...)` for maintaining files
- `@MergeMap(...)` or `@MergeList(...)` when collection behavior matters
