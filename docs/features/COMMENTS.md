## Comments

The comment feature writes comments declared on the model into the files Configura writes, so a
user's file keeps explaining its settings after every update.

### Wiring

```kotlin
implementation("me.whereareiam.configura.feature:comment:<version>")
```

```java
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.comment.CommentFeature;

Configura configura = Configura.builder()
		.feature(CommentFeature.defaults())
		.build();
```

### Declaring comments

```java
import me.whereareiam.configura.feature.comment.api.annotation.Comment;

@Comment("Settings of the lobby server.")
public class Settings {
	@Comment({"Seconds before an idle player is moved.", "0 disables it."})
	public int timeout = 30;

	public Retry retry = new Retry();
}

public class Retry {
	@Comment("How often a request is retried.")
	public int attempts = 3;
}
```

`configura.update("settings", Settings.class)` then writes:

```yaml
# Settings of the lobby server.

# Seconds before an idle player is moved.
# 0 disables it.
timeout: 30
retry:
  # How often a request is retried.
  attempts: 3
```

On the document class, the lines open the file, followed by an empty line. On a field, they are written above its key wherever
the field's class appears: in a section, in every entry of a map or list, and in a subtype another
feature picks. An empty string writes an empty comment line.

### Which writes get comments

| Format | Comments |
| --- | --- |
| YAML | `#` lines |
| JSON with indentation (the default) | `//` lines, which Configura reads back |
| JSON without indentation, formats added with `format(extension, mapperFactory)` | none |

`update`, `save`, `write` and `writeBytes` know the model and write its comments. `writeNode` writes a
tree without a model and writes none. A document whose model declares no comments is written exactly
as without the feature.

The comments come from the model, not from the file: a comment a user adds by hand is gone after the
next write.
