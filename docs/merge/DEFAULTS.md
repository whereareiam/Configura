## Defaults Resolution

Defaults are seed data for merge.

They are core to Configura, but they do not own shape semantics. Merge and type adapters decide how defaults are consumed for values, objects, maps, and lists.

### When defaults are used

Defaults are applied by:
- `save(...)`
- `merge(...)`
- `update(...)`

Defaults are not applied by:
- `read(...)`
- `readNode(...)`
- `readResolvedNode(...)`

### Built-in Defaults DSL

Scalar defaults:

```java
import me.whereareiam.configura.annotation.merge.MergeValue;

public class AppConfig {
	@MergeValue(text = "localhost")
	public String host;
}
```

Object defaults:

```java
import me.whereareiam.configura.annotation.merge.MergeObject;

public class RetryHolder {
	@MergeObject(properties = {
			@MergeObject.Property(name = "attempts", number = "3")
	})
	public RetryConfig retry;
}
```

List defaults:

```java
import me.whereareiam.configura.annotation.merge.MergeList;

import java.util.List;

public class RolesConfig {
	@MergeList(items = {
			@MergeList.Item(text = "user"),
			@MergeList.Item(text = "admin")
	})
	public List<String> roles;
}
```

### Source-Backed Defaults

```java
import me.whereareiam.configura.annotation.merge.MergeDefaultsSource;

public class AppConfig {
	@MergeDefaultsSource("classpath:/defaults/app-host.json")
	public String host;
}
```

### Provider Defaults

```java
import me.whereareiam.configura.merge.defaults.DefaultsProvider;

public final class AppDefaults implements DefaultsProvider<AppConfig> {
	@Override
	public AppConfig supply(AppConfig config) {
		config.name = "service";
		return config;
	}
}
```

Register it:

```java
Config.builder()
		.defaults(AppDefaults.class)
		.build();
```

Or declare it:

```java
import me.whereareiam.configura.annotation.merge.DefaultsProvider;

@DefaultsProvider(AppDefaults.class)
public class AppConfig {
	public String name;
}
```

### Custom Defaults Resolvers

The built-in annotations are only one frontend. You can resolve defaults programmatically:

```java
Config.builder()
		.defaultsResolver((descriptor, context) -> {
			if ("host".equals(descriptor.getSerializedName()))
				return descriptor.getMapper().valueToTree("localhost");
			return null;
		})
		.build();
```
