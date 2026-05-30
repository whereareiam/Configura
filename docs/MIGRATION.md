## Migration

Migration is a core Configura capability for structural document changes across versions.

Use migrations when defaults are not enough, for example when you need to:
- rename a field
- move data to a new location
- split one field into several fields
- remove or normalize legacy shapes

### Version fields

Configura resolves document versions from:
- a field annotated with `@DocumentVersion`
- `ConfigDocument.version`
- a serialized `_version` field as a fallback

Convenience base class:

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import me.whereareiam.configura.annotation.DocumentVersion;

public abstract class ConfigDocument {
	@DocumentVersion
	@JsonProperty("_version")
	protected Integer version;
}
```

Custom version field:

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import me.whereareiam.configura.annotation.DocumentVersion;

public final class Settings {
	@DocumentVersion
	@JsonProperty("documentVersion")
	public Integer documentVersion;
}
```

### Migration step

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.migration.ConfigMigrationContext;
import me.whereareiam.configura.migration.ConfigMigrationStep;

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
		JsonNode legacy = connection.remove("legacyHost");
		if (legacy != null && !legacy.isNull())
			connection.set("host", legacy);
		return root;
	}
}
```

### Registration

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.type.Format;

Configura config = Config.builder()
		.format(Format.YAML)
		.versioned(Settings.class, spec -> spec
				.currentVersion(1)
				.migration(new SettingsMigrationV0ToV1()))
		.build();
```

### Behavior

- missing version is treated as `0`
- `read(...)` migrates in memory only
- `update(...)` migrates and persists the upgraded file
- `save(...)` upgrades existing on-disk content before saving
- by default, persisted migrations create `*.bak` backups

Disable backups if needed:

```java
Configura config = Config.builder()
		.backupOnMigration(false)
		.versioned(Settings.class, spec -> spec.currentVersion(1))
		.build();
```
