## Versioned Config Migrations

Use versioned migrations when template defaults are not enough.

Templates are still the right tool when you only need to:
- add a new field with a sensible default
- fill in missing nested values
- preserve explicit user values while enriching the model

Migrations are for structural changes such as:
- renaming a field
- moving a value to a new location
- splitting one field into several fields
- collapsing or removing legacy shapes

## How It Works

Configura can register a versioned root config type and a chain of migration classes. Each migration class handles
one forward step for one root type, such as `0 -> 1` or `1 -> 2`.

By default, Configura resolves version from the model first:

- a field annotated with `@SchemaVersion`
- the inherited `ConfigDocument.version` field
- any model field serialized as `_version`
- raw tree `_version` as a fallback for existing configs
- otherwise version `0`

If the model has no version field and the source tree also has no `_version`, Configura still migrates from version `0`
but does not persist a version field back to disk.

- missing version is treated as version `0`
- `read(...)` migrates in memory only
- `update(...)` migrates and persists the upgraded file
- `save(...)` upgrades existing on-disk content before saving
- `readNode(...)` stays raw
- `readMigratedNode(..., Type.class)` returns the upgraded tree without writing it

## Basic Example

Legacy version `0`:

```yaml
connection:
  routing:
    defaultProxy: proxy-auth
```

Target version `1`:

```yaml
_version: 1
connection:
  routing:
    defaults:
      step:
        target: proxy-auth
```

Migration step:

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.migration.ConfigMigrationStep;
import me.whereareiam.configura.migration.ConfigMigrationContext;

public final class SettingsMigrationV0ToV1 implements ConfigMigrationStep<Settings> {
	@Override
	public Class<Settings> type() {
		return Settings.class;
	}

	@Override
	public int fromVersion() {
		return 0;
	}

	@Override
	public int toVersion() {
		return 1;
	}

	@Override
	public ObjectNode migrate(ObjectNode root, ConfigMigrationContext context) {
		ObjectNode connection = context.object(root, "connection");
		ObjectNode routing = context.object(connection, "routing");

		JsonNode defaultProxy = routing.remove("defaultProxy");
		if (defaultProxy != null && !defaultProxy.isNull()) {
			ObjectNode defaults = context.object(routing, "defaults");
			ObjectNode step = context.object(defaults, "step");
			step.set("target", defaultProxy);
		}

		return root;
	}
}
```

Registration:

```java
public final class Settings extends ConfigDocument {
	public Connection connection = new Connection();
}

Config config = Config.builder()
		.format(Format.YAML)
		.versioned(Settings.class, spec -> spec
				.currentVersion(1)
				.migration(new SettingsMigrationV0ToV1()))
		.build();
```

`ConfigDocument` is the convenience default:

```java
public abstract class ConfigDocument {
	@SchemaVersion
	@JsonProperty("_version")
	protected Integer version;
}
```

If you do not want to extend `ConfigDocument`, you can declare your own version field:

```java
public final class Settings {
	@SchemaVersion
	@JsonProperty("schemaVersion")
	public Integer schemaVersion;
}
```

## Multiple Versions

For long-lived configs, keep one class per historical step and register the whole chain.

```java
Config config = Config.builder()
		.format(Format.YAML)
		.versioned(Settings.class, spec -> spec
				.currentVersion(3)
				.migration(new SettingsMigrationV0ToV1())
				.migration(new SettingsMigrationV1ToV2())
				.migration(new SettingsMigrationV2ToV3()))
		.build();
```

Configura resolves and executes the steps by `fromVersion()`. Registration order does not matter, but listing them in
natural order keeps the setup readable.

Recommended package layout for plugin consumers such as Identica:

```text
identica-common/src/main/java/.../config/migration/settings/
  SettingsMigrationV0ToV1.java
  SettingsMigrationV1ToV2.java
  SettingsMigrationV2ToV3.java
```
