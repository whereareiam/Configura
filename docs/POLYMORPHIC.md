## Polymorphic Models

Polymorphic support lets you bind a base model to different concrete subtypes based on a discriminator field, while writing only the fields relevant to the active subtype.

Use this for clean configs when models are dynamic (e.g., different variants selected by `type`).

### Annotation-based

```java
import me.whereareiam.configura.annotation.Polymorphic;

@Polymorphic(
        discriminator = "type",
        mappings = {
                @Polymorphic.Type(value = "SYMBOL",  target = SymbolTrigger.class),
                @Polymorphic.Type(value = "REGEX",   target = RegexTrigger.class),
                @Polymorphic.Type(value = "COMMAND", target = CommandTrigger.class)
        },
        defaultValue = "SYMBOL"
)
class TriggerBase {
    public String type;
    public boolean strip;
    public Integer radius;
}

class SymbolTrigger extends TriggerBase { public String symbol; }
class RegexTrigger extends TriggerBase  { public String pattern; }
class CommandTrigger extends TriggerBase { public String command; }
```

Read/write usage is unchanged:

```java
TriggerBase cfg = Config.load("trigger", TriggerBase.class);
Config.save("trigger", cfg);
```

Example YAML:

```yaml
type: SYMBOL
symbol: "#"
strip: true
radius: 10
```

When `type: COMMAND`, only `command` (plus shared fields) are written; unrelated fields like `symbol`/`pattern` are omitted.

### Inference-only (no discriminator)

If you don’t want a `type` field, you can register inference rules that select the subtype by the presence of a field. First match wins; you can also define an optional default.

Builder setup:

```java
// Base and variants
class InferBase {}
class InferServers extends InferBase { public List<String> servers; }
class InferWorlds  extends InferBase { public List<String> worlds; }

// Register inference rules (no discriminator, no mappings)
Config.registerPolymorphic(InferBase.class)
        .inferByField("servers", InferServers.class)
        .inferByField("worlds",  InferWorlds.class)
        // .defaultTarget(InferServers.class) // optional
        .build();
```

Examples (YAML):

```yaml
servers: ["s1", "s2"]   # → InferServers
```

```yaml
worlds: ["overworld"]    # → InferWorlds
```

Notes:
- Writer emits only fields of the concrete subtype; no discriminator is written.
- If multiple rules could match, the first rule (registration order) wins.
- If nothing matches and no `defaultTarget` is set, binding falls back to the base type.

### Dual-mode (discriminator + inference)

You can combine both approaches: when a discriminator is present it takes priority; otherwise inference rules apply.

```java
Config.registerPolymorphic(TriggerBase.class)
        .discriminator("type")
        .map("SYMBOL",  SymbolTrigger.class)
        .map("REGEX",   RegexTrigger.class)
        // inference as fallback if 'type' is missing
        .inferByField("symbol",  SymbolTrigger.class)
        .inferByField("pattern", RegexTrigger.class)
        .defaultValue("SYMBOL")
        .build();
```

Behavior:
- Read: use `type` if present; else apply `inferByField`; else use `defaultValue`/`defaultTarget` if configured.
- Write: only concrete subtype fields (and shared base fields) are written; the discriminator is written only if configured.

### Builder-based

If you prefer programmatic setup instead of annotations:

```java
Config.registerPolymorphic(TriggerBase.class)
        .discriminator("type")
        .map("SYMBOL",  SymbolTrigger.class)
        .map("REGEX",   RegexTrigger.class)
        .map("COMMAND", CommandTrigger.class)
        .defaultValue("SYMBOL")
        .build();
```

Then read/write normally via `Config` or a `ConfigReader/ConfigWriter`.

### Behavior

- Discriminator selection: On read, the value of the discriminator (e.g., `type`) selects the concrete subtype. If missing/unknown, `defaultValue` is used when provided.
- Omit irrelevant fields: On write, only fields of the active subtype (and shared base fields) are written. Fields belonging to other variants are not emitted.
- Works for YAML and JSON.

### Tips

- Keep the discriminator stable to avoid migrations.
- Prefer simple, uppercase mapping keys (e.g., `SYMBOL`).
- You can mix polymorphic models with `TypeAdapter`s; they are independent features.


