## Templating Guide

Templates let you declare default values directly on fields of your config classes or source them from external
providers/resources. These defaults can be materialized into configuration files or used during merges when you
update-and-read.

### When templates are used

- During update-and-read, e.g. `Config.updateRead("app-config", new AppConfig())`, templates help seed missing values.
- During writes followed by reads, if your policy merges on update.

Note: YAML is the default format when no extension is provided; JSON is also supported via `.json`.

### Annotations

- `@Template` — placed on a field to describe inline defaults.
- `@Source` — populate a field from an external template (classpath/URL/file).
- `@Supplier` — use a `TemplateProvider` to generate a default instance.
- `@Literal` — simple value item for defaults.
- `@TemplateList` — list of `@Literal` items.
- `@TemplateObject` — object-like value composed of named `@Property` entries.
- `@Property` — a named entry used inside `@TemplateObject` or `@Template(properties=...)`.

Imports used in the examples:

```java
import lombok.Data;
import me.whereareiam.configura.annotation.template.Template;
import me.whereareiam.configura.annotation.template.Source;
import me.whereareiam.configura.annotation.template.Supplier;
import me.whereareiam.configura.annotation.template.type.Literal;
import me.whereareiam.configura.annotation.template.type.Property;
import me.whereareiam.configura.annotation.template.type.TemplateList;
import me.whereareiam.configura.annotation.template.type.TemplateObject;
```

### Simple value default

```java

@Data
public class GreetingConfig {
	@Template(literal = @Literal(text = "world"))
	private String name;
}
```

### List default

```java
import java.util.List;

@Data
public class RolesConfig {
	@Template(items = {@Literal(text = "user"), @Literal(text = "admin")})
	private List<String> roles;
}
```

<details>
  <summary>Using @TemplateList directly</summary>

  ```java
  import lombok.Data;

import java.util.List;

import me.whereareiam.configura.annotation.template.type.TemplateList;
import me.whereareiam.configura.annotation.template.type.Literal;

@Data
public class FeaturesConfig {
	@TemplateList(items = {@Literal(text = "chat"), @Literal(text = "metrics")})
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
			@Property(name = "host", value = @Literal(text = "localhost")),
			@Property(name = "port", value = @Literal(number = "5432"))
	})
	private Map<String, Object> defaults;
}
```

<details>
  <summary>Using @TemplateObject directly</summary>

  ```java
  import lombok.Data;

import java.util.Map;

import me.whereareiam.configura.annotation.template.type.TemplateObject;
import me.whereareiam.configura.annotation.template.type.Property;
import me.whereareiam.configura.annotation.template.type.Literal;

@Data
public class ApiConfig {
	@TemplateObject(properties = {
			@Property(name = "baseUrl", value = @Literal(text = "https://api.example.com")),
			@Property(name = "timeoutSeconds", value = @Literal(number = "10"))
	})
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
	@Template(literal = @Literal(text = "MyApp"))
	private String appName;

	@Template(properties = {
			@Property(name = "host", value = @Literal(text = "127.0.0.1")),
			@Property(name = "port", value = @Literal(number = "8080"))
	})
	private Map<String, Object> server;

	@Template(items = {@Literal(text = "user"), @Literal(text = "admin")})
	private List<String> roles;
}
  ```

</details>

### Applying templates in code

```java
import me.whereareiam.configura.Config;

AppConfig cfg = new AppConfig();
cfg =Config.

updateRead("app-config",cfg); // seeds missing values from templates
```

### External templates with @Source

Populate a field from a classpath resource, URL, or file.

```java

@Data
public class FromResourceConfig {
	@Source("classpath:/defaults/app.yaml")
	private Map<String, Object> defaults;
}
```

### Programmatic templates with @Supplier

Provide defaults via a `TemplateProvider` implementation.

```java
public class AppDefaultsProvider implements TemplateProvider<AppConfig> {
	@Override
	public AppConfig get() {
		AppConfig cfg = new AppConfig();
		cfg.setAppName("MyApp");
		return cfg;
	}
}

@Data
public class AppConfig {
	@Supplier(AppDefaultsProvider.class)
	private AppConfig defaults;
}
```

### Notes and tips

- Prefer concise templates that reflect sensible defaults.
- For object-like defaults, keep property names stable to avoid churn in files.
- Use YAML for readability; switch to JSON by using a `.json` file extension.


