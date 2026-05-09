## Templating Guide

Templates let you declare default values directly on fields or classes. These defaults are materialized into configuration files during smart writes (save/apply).

### When templates are used

- During smart writes, e.g. `Config.defaults().save("app-config", new AppConfig())`, templates help seed missing values.
- Reading does not apply templates; it strictly deserializes the file.

Note: YAML is the default format when no extension is provided; JSON is also supported via `.json`.

### Annotation

- `@Template` — can be used on fields or classes; contains nested types: `Template.Literal`, `Template.Property`, `Template.List`, `Template.Object`, `Template.Source`, `Template.Supplier`.

Imports used in the examples:

```java
import lombok.Data;
import me.whereareiam.configura.annotation.Template;
import me.whereareiam.configura.TemplateProvider;
```

### Simple value default

```java
@Data
public class GreetingConfig {
	@Template(text = "world")
	private String name;
}
```

### List default

```java
import java.util.List;

@Data
public class RolesConfig {
	@Template(stringItems = {"user", "admin"})
	private List<String> roles;
}
```

<details>
  <summary>Using the namespaced list</summary>

  ```java
  @Data
  public class FeaturesConfig {
	  @Template(list = @Template.List(items = {
		  @Template.Literal(text = "chat"),
		  @Template.Literal(text = "metrics")
	  }))
	  private List<String> enabled;
  }
  ```

</details>

### Object-like (map) default

```java
import java.util.Map;

@Data
public class DbConfig {
	@Template(properties = {
			@Template.Property(name = "host", text = "localhost"),
			@Template.Property(name = "port", number = "5432")
	})
	private Map<String, Object> defaults;
}
```

<details>
  <summary>Using the namespaced object</summary>

  ```java
  @Data
  public class ApiConfig {
	  @Template(object = @Template.Object(properties = {
			  @Template.Property(name = "baseUrl", text = "https://api.example.com"),
			  @Template.Property(name = "timeoutSeconds", number = "10")
	  }))
	  private Map<String, Object> http;
  }
  ```

</details>

### Nested structures

You can combine templates on multiple fields to shape nested structures in the final file. Keep templates close to the
fields they describe for clarity.

<details>
  <summary>Example: nested config</summary>

  ```java
  import java.util.Map;
  import java.util.List;

  @Data
  public class AppConfig {
    @Template(text = "MyApp")
    private String appName;

    @Template(properties = {
            @Template.Property(name = "host", text = "127.0.0.1"),
            @Template.Property(name = "port", number = "8080")
    })
    private Map<String, Object> server;

    @Template(stringItems = {"user", "admin"})
    private List<String> roles;
  }
  ```

</details>

### Applying templates in code

```java
import me.whereareiam.configura.Config;

Config config = Config.builder().build();

AppConfig cfg = new AppConfig();
config.save("app-config", cfg);
cfg = config.read("app-config", AppConfig.class);
```

### External templates with Template.Source

Populate a field from a classpath resource, URL, or file.

```java
@Data
public class FromResourceConfig {
	@Template(source = @Template.Source("classpath:/defaults/app.yml"))
	private Map<String, Object> defaults;
}
```

### Programmatic templates with Template.Supplier

Provide defaults via a `TemplateProvider` implementation.

```java
public class AppDefaultsProvider implements TemplateProvider<AppConfig> {
	@Override
	public AppConfig supply(AppConfig cfg) {
		cfg.setAppName("MyApp");
		return cfg;
	}
}

@Data
public class AppConfig {
	@Template(supplier = @Template.Supplier(AppDefaultsProvider.class))
	private AppConfig defaults;
}
```

### Notes and tips

- Prefer concise templates that reflect sensible defaults.
- For object-like defaults, keep property names stable to avoid churn in files.
- Use YAML for readability; switch to JSON by using a `.json` file extension.


## Merge Policies: controlling template behavior

Use `@Merge` to select a built-in merge preset, or register custom named policies in code through `Config.builder()`.

### Available Presets

#### `DEEP_DEFAULTS` (default)
Recursively merges nested structures. Template adds missing keys while preserving user changes.

**Use for:** Nested config objects where you want to add new template keys over time.

```java
@Data
public class ServerConfig {
    @Merge
    private DatabaseSettings database;
}
```

**Behavior:**
- Template has: `{ host: "localhost", port: 5432 }`
- User has: `{ host: "example.com" }`
- Result: `{ host: "example.com", port: 5432 }`

---

#### `SOURCE_OWNS_FIELD`
Only applies template if field is completely missing from user config. Once user has any value (even empty), template is ignored.

**Use for:** Maps/Lists where users should control all entries (commands, languages, feature flags).

```java
@Data
public class CommandsConfig {
    @Merge(preset = MergePreset.SOURCE_OWNS_FIELD)
    private Map<String, CommandDefinition> commands;
}
```

**Behavior:**
- Template has: `{ help: {...}, reload: {...} }`
- User has: `{ help: {...} }` (deleted reload)
- Result: `{ help: {...} }` (reload NOT re-added)

---

#### `NEVER_TEMPLATE`
Template is never applied. Field is pure user data.

**Use for:** User-specific data with no template defaults.

```java
@Data
public class UserPreferences {
    @Merge(preset = MergePreset.NEVER_TEMPLATE)
    private String theme;
}
```

**Behavior:**
- Template has: `theme: "dark"`
- User has: (nothing)
- Result: `null` (template NOT applied)

---

#### `DECLARED_KEYS_ONLY_MAP`
Only merge defaults into keys the source already declared.

**Use for:** Maps where the field should stay implicit unless the user opts in by creating a key.

**Behavior:**
- Template has: `{ authentication: { step: "auth", complete: "lobby" }, registration: {...} }`
- User has: `{ authentication: { complete: "" } }`
- Result: `{ authentication: { step: "auth", complete: "" } }`

### Explicit null handling

Explicit source `null` is preserved by default during merge/update. Templates do not reapply over an explicit `null` unless a merge policy is designed to say otherwise.

### Global defaults and named policies

You can change the default policy for unannotated fields or register named policies for specific fields.

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.Merge;
import me.whereareiam.configura.merge.MergePolicy;
import me.whereareiam.configura.type.MergePreset;

Config config = Config.builder()
        .defaultMergePreset(MergePreset.SOURCE_OWNS_FIELD)
        .mergePolicy("declaredKeysOnly", MergePolicy.builder()
                .mapMode(MergePolicy.MapMode.DECLARED_SOURCE_KEYS_ONLY)
                .build())
        .build();

public class FlowConfig {
    @Merge(policy = "declaredKeysOnly")
    private Map<String, Step> scenarios;
}
```

**Example YAML:**
```yaml
commands:
  status:
    enabled: true
    requirements: null  # User explicitly disabled requirements
  
  help:
    enabled: true
    # No requirements field - template will provide default if it exists
```
