## Extensions

The extension feature lets you resolve a declared base document type to a subtype. 

### Mark extendable types or fields

```java
import me.whereareiam.configura.feature.extension.api.annotation.ExtendableDocument;

@ExtendableDocument
public class CommandDefinition {
	public boolean enabled;
}
```

Or mark the field instead of the type:

```java
public class Wrapper {
	@ExtendableDocument
	public CommandDefinition command;
}
```

### Whole-document rule

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.extension.ExtensionFeature;
import me.whereareiam.configura.feature.extension.api.ConfigDocumentRule;

Configura config = Config.builder()
		.feature(ExtensionFeature.rule(
				ConfigDocumentRule.whole(
						CommandDefinition.class,
						SecureCommandDefinition.class
				)
		))
		.build();
```

### Contextual rule

Contextual rules use a predicate over `DocumentTypeContext`.

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.extension.ExtensionFeature;
import me.whereareiam.configura.feature.extension.api.ConfigDocumentRule;

Configura config = Config.builder()
		.feature(ExtensionFeature.rule(
				ConfigDocumentRule.when(
						CommandDefinition.class,
						SecureCommandDefinition.class,
						context -> "login".equals(context.getMapKey())
				)
		))
		.build();
```

Example based on a sibling field:

```java
ConfigDocumentRule.when(
		ProviderEntry.class,
		CorporateProviderEntry.class,
		context -> {
			if (context.getCurrentNode() == null || !context.getCurrentNode().isObject()) return false;
			var id = context.getCurrentNode().get("id");
			return id != null && id.isValueNode() && "corporate".equals(id.asText());
		}
)
```
