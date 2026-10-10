## Polymorphic Models

The polymorphic feature resolves a base type to a subtype based on a discriminator or inference rules.

### Annotation-based polymorphism

```java
import me.whereareiam.configura.feature.polymorphic.api.annotation.Polymorphic;

@Polymorphic(
		discriminator = "type",
		mappings = {
				@Polymorphic.Type(value = "SYMBOL", target = SymbolTrigger.class),
				@Polymorphic.Type(value = "REGEX", target = RegexTrigger.class),
				@Polymorphic.Type(value = "COMMAND", target = CommandTrigger.class)
		},
		defaultValue = "SYMBOL"
)
class TriggerBase {
	public String type;
}
```

Wire the feature:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.polymorphic.PolymorphicFeature;

Configura config = Config.builder()
		.feature(PolymorphicFeature.defaults())
		.build();
```

### Builder-based registration

```java
PolymorphicFeature feature = PolymorphicFeature.defaults();
feature.register(TriggerBase.class)
		.discriminator("type")
		.map("SYMBOL", SymbolTrigger.class)
		.map("REGEX", RegexTrigger.class)
		.defaultValue("SYMBOL")
		.build();

Configura config = Config.builder()
		.feature(feature)
		.build();
```

### Inference-only

```java
feature.register(InferBase.class)
		.inferByField("servers", InferServers.class)
		.inferByField("worlds", InferWorlds.class)
		.build();
```

### How a subtype is chosen

For each document node bound to the base type, the first rule that applies decides:

1. The node has the discriminator as text: the mapping of that value.
2. The node has no discriminator, or one that is not text: the mapping of `defaultValue`.
3. The first `inferBy` field the node has.
4. `defaultTarget`.

Values are matched exactly, including case.

A base type may be a target of its own mapping, and a base type that is not abstract is also what
a node without a discriminator is bound to when no rule applies.

### When no subtype can be chosen

Loading fails with a `ConfigException` instead of guessing. Inside a larger document the message
says where the node is.

| The node | Message |
| --- | --- |
| names a value without a mapping, and neither inference nor `defaultTarget` applies | `Unknown 'type' value 'ORACLE' for com.example.Storage at /storages/1; accepted values: SQLITE, POSTGRES` |
| has no discriminator, no rule applies, and the base type is abstract or an interface | `Cannot choose a subtype of com.example.Storage: 'type' is missing; accepted values: SQLITE, POSTGRES` |
| has none of the inference fields of a base type without a discriminator, and that type is abstract or an interface | `Cannot choose a subtype of com.example.Scope: it has none of the fields servers, worlds` |

The exception is the cause of the `ConfigException` that `read`, `update` and `merge` throw for the
file; for a node inside a document Jackson adds the property chain in between.
