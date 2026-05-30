## Strategies

Strategies are executable field merge behaviors.

They answer:
- when source wins
- when defaults win
- whether missing children should be deep-filled
- whether a subtree should stay absent until declared

Configura keeps strategies class-based.

### Built-in Strategies

- `DeepDefaults`
- `SourceOwnsField`
- `NeverDefaults`
- `StructuralObject`
- `DeclaredObjectDefaults`

Example:

```java
import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.merge.strategy.SourceOwnsField;

public class CommandsConfig {
	@Merge(SourceOwnsField.class)
	public CommandDefinition command;
}
```

Named strategies are also supported:

```java
@Merge(named = "sourceOwnsField")
public CommandDefinition command;
```

### Custom Strategies

Implement `FieldMergeStrategy` when you need a custom field merge rule:

```java
import com.fasterxml.jackson.databind.JsonNode;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;

public final class AlwaysDefault implements FieldMergeStrategy {
	@Override
	public JsonNode merge(MergeContext context) {
		return context.getDefaultNode();
	}
}
```

Register it:

```java
Config.builder()
		.mergeStrategy("alwaysDefault", AlwaysDefault.class)
		.build();
```

Or register a richer definition:

```java
Config.builder()
		.mergeStrategy(MergeStrategyDefinition.builder(AlwaysDefault.class)
				.alias("alwaysDefault")
				.build())
		.build();
```
