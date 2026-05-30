## Annotation DSL

Configura’s built-in declarative DSL is grouped under `me.whereareiam.configura.annotation.merge`.

### `@Merge`

Selects a merge strategy:

```java
import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;

public class HostConfig {
	@Merge(SourceOwnsField.class)
	public String host;
}
```

### `@MergeValue`

Declares scalar defaults:

```java
@MergeValue(text = "localhost")
public String host;
```

### `@MergeObject`

Declares object/subtree defaults:

```java
@MergeObject(properties = {
		@MergeObject.Property(name = "attempts", number = "3")
})
public RetryConfig retry;
```

### `@MergeMap`

Declares map behavior and optional seeded entries:

```java
@MergeMap(
		presence = MapPresence.DECLARED_ONLY,
		unknownEntries = MapUnknownEntries.ALLOW
)
public Map<String, Scenario> scenarios;
```

### `@MergeList`

Declares list behavior and optional seeded items:

```java
@MergeList(
		mode = ListMode.KEYED,
		key = "id",
		presence = ListPresence.DECLARED_ONLY,
		unknownEntries = ListUnknownEntries.ALLOW
)
public List<ProviderEntry> providers;
```
