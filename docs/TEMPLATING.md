## Templating Guide

Templates let you declare default values directly on fields or classes. These defaults are materialized into configuration files during smart writes (save/apply).

### When templates are used

- During smart writes, e.g. `Config.save("app-config", new AppConfig())`, templates help seed missing values.
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

AppConfig cfg = new AppConfig();
Config.save("app-config", cfg);
cfg = Config.read("app-config", AppConfig.class);
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


## Policy: controlling merge behavior

Use `me.whereareiam.configura.annotation.Policy` on types or fields to tune how smart writes merge templates with existing files:

- `mergeOnUpdate` (type-level): when `false`, update-read avoids rewriting the file and loads it as-is.
- `skipMerge` (field-level): when `true`, do not add defaults for that field if it is missing or null in the existing file.
- `preserveWrite` (field-level): when `true` and the existing file has a non-null value for the field, keep that subtree as-is (no deep merge or additions).

Additional rules:

- Existing user values win by default. Templates fill only missing or null values.
- `skipMerge` treats null as missing (so templates are not added for that field when null/missing).
- `preserveWrite` only applies when the existing value is non-null; nulls are treated as missing so templates can supply defaults.

Example:

```java
public class Settings {
    @Policy(preserveWrite = true)
    private Synchronization synchronization;

    public static class Synchronization {
        @Policy(skipMerge = false)
        private String server; // if null or missing, template will provide a default
    }
}
```