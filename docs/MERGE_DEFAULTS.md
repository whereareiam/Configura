## Merge Defaults

`@Defaults` declares defaults for the merge engine. It is not a separate runtime feature: defaults are discovered by merge, then the selected merge strategy decides how they apply to source data.

### When defaults are used

- `Config.save(...)`, `Config.merge(...)`, and `Config.update(...)` use merge defaults.
- `Config.read(...)` strictly deserializes existing data and does not apply defaults.
- YAML is the default format when no extension is provided; JSON is also supported via `.json`.

### Inline defaults

```java
import lombok.Data;
import me.whereareiam.configura.annotation.Defaults;

@Data
public class GreetingConfig {
    @Defaults(text = "world")
    private String name;
}
```

Lists and object-like defaults are also declared with `@Defaults`:

```java
import java.util.List;
import java.util.Map;

public class AppConfig {
    @Defaults(stringItems = {"user", "admin"})
    public List<String> roles;

    @Defaults(properties = {
            @Defaults.Property(name = "host", text = "localhost"),
            @Defaults.Property(name = "port", number = "5432")
    })
    public Map<String, Object> database;
}
```

### Provider defaults

Register reusable model defaults through merge:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.merge.MergeDefaultsProvider;

public class AppDefaults implements MergeDefaultsProvider<AppConfig> {
    @Override
    public AppConfig supply(AppConfig config) {
        config.name = "MyApp";
        return config;
    }
}

Config config = Config.builder()
        .defaults(AppDefaults.class)
        .build();
```

Class-level and field-level provider defaults can still be declared with `@Defaults.Provider`:

```java
@Defaults(provider = @Defaults.Provider(AppDefaults.class))
public class AppConfig {
    public String name;
}
```

### Merge strategies

Use `@Merge` to select a strategy class per field, or register named custom strategies in code.

Built-in strategies:

- `DeepDefaults`: recursively fill missing values from defaults.
- `SourceOwnsField`: if the source provides a value, source owns the whole field.
- `NeverDefaults`: never apply defaults for this field.
- `DeclaredKeysOnlyMap`: only merge into map keys already declared by source.
- `StructuralObject`: keep an object present without deep-filling declared children.

```java
import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.merge.strategy.DeclaredKeysOnlyMap;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;

public class RoutingConfig {
    @Merge(DeclaredKeysOnlyMap.class)
    public Map<String, Scenario> scenarios;

    @Merge(SourceOwnsField.class)
    public Map<String, Command> commands;
}
```

### Named custom strategies

```java
import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.MergeStrategy;

public final class AlwaysDefault implements MergeStrategy {
    @Override
    public JsonNode merge(MergeContext context) {
        return context.getDefaultNode();
    }
}

Config config = Config.builder()
        .mergeStrategy("alwaysDefault", AlwaysDefault.class)
        .build();

public class FeatureConfig {
    @Merge(named = "alwaysDefault")
    @Defaults(text = "enabled")
    public String mode;
}
```

Explicit source `null` is preserved by default during merge/update.
