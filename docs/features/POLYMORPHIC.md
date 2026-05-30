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
