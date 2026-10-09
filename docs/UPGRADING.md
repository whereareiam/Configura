# Upgrading from 1.0

This release is breaking. Most applications are affected in three places: models that extend
`ConfigDocument`, fields with merge annotations, and calls to the static methods of `Config`.
Everything not listed here is unchanged, including `Config.configured()`, `Config.setConfigured()`,
`Config.builder()`, `withDefaults`, `withFeature`, `update`, `read`, `writeBytes`,
`DefaultsProvider` classes and `@PreserveUnknownFields`.

## Models

| 1.0 | Now |
| --- | --- |
| `class Settings extends ConfigDocument` | Remove the superclass. Configura has no version field any more. If you migrate configurations with Strata, register its `StrataFeature`; the version is then kept in the file without being part of the model. |
| `@DocumentVersion` | Removed. |

## Merge annotations

All merge options are on `@Merge`. `@MergeList`, `@MergeMap` and the strategy classes are removed.

| 1.0 | Now |
| --- | --- |
| `@Merge` or `@Merge(DeepDefaults.class)` | No annotation |
| `@Merge(SourceOwnsField.class)`, `@Merge(StructuralObject.class)` | `@Merge(present = WhenPresent.KEEP_AS_WRITTEN)` |
| `@Merge(DeclaredObjectDefaults.class)` | `@Merge(absent = WhenAbsent.LEAVE_ABSENT)` |
| `@Merge(NeverDefaults.class)` | `@Merge(absent = WhenAbsent.LEAVE_ABSENT, present = WhenPresent.KEEP_AS_WRITTEN)` |
| `@MergeList(mode = ListMode.KEYED, key = "id")` | `@Merge(key = "id")` |
| `@MergeList(mode = ListMode.PLAIN)` | No annotation |
| `presence = DECLARED_ONLY` on a list or map | `missingEntries = MissingEntries.OMIT` |
| `presence = SEED_DEFAULTS` or `DEFAULT_DOMAIN_ONLY` | Nothing; adding is the default |
| `unknownEntries = REJECT` | `unknownEntries = UnknownEntries.REJECT` |

`@MergeList` and `@MergeMap` used `DECLARED_ONLY` when `presence` was not given. The new default is
to add missing entries, so a field that relied on the old default needs
`missingEntries = MissingEntries.OMIT`.

```java
// 1.0
@Merge
@MergeList(mode = ListMode.KEYED, key = "id", presence = ListPresence.DECLARED_ONLY)
private List<ProviderEntry> providers;

// now
@Merge(key = "id", missingEntries = MissingEntries.OMIT)
private List<ProviderEntry> providers;
```

## Defaults declared in annotations

| 1.0 | Now |
| --- | --- |
| `@MergeValue(text = "x")`, `@Defaults(text = "x")` | A field initializer: `public String name = "x";` |
| `@MergeObject(properties = …)`, `@MergeList(items = …)`, `@MergeMap(entries = …)` | A field initializer or a `DefaultsProvider` |
| `@DefaultsProvider(X.class)` on a class or field | Register the provider: `.defaults(X.class)` or `withDefaults(X.class)` |
| `@MergeDefaultsSource("classpath:…")` | A `DefaultsProvider` that reads the resource |

## Entry points

| 1.0 | Now |
| --- | --- |
| `Config.update(…)`, `Config.read(…)` and the other static delegates | The same method on an instance: `Config.configured().update(…)` or `Config.yaml().update(…)` |
| `Config.reader(Format.JSON)`, `Config.writer(…)` | `Config.json()` and its `read`, `readNode`, `write` methods |
| `Config.configure(builder -> …)` | `Config.setConfigured(Config.configured().toBuilder()….build())` |
| `Config.Builder` | `Configura.Builder`; `Config.builder()` returns one |
| `withModule`, `withMergeBehavior` | `toBuilder().module(…).build()`, `toBuilder().mergeBehavior(…).build()` |
| `readResolvedNode(…)` | `readNode(…)` |
| `prepareNode(…)` | Removed |

`Config.builder()`, `Config.yaml()` and `Config.json()` used to start from the shared instance once
one had been set. They now always start fresh; use `Config.configured().toBuilder()` to build on the
shared one.

## Extension points that are gone

Custom merge strategies, `MergeTypeAdapter`, `MergePolicyResolver`, `DefaultsResolver`, and the
builder methods `mergeStrategy`, `mergeTypeAdapter`, `defaultsResolver`, `policyResolver` and
`defaultStrategy` are removed without replacement. Type resolution, binding hooks and Jackson
modules are still extensible through [features](FEATURES.md).

## Behaviour that changed

- An empty list in a user's file (`servers: []`) is kept. It used to be replaced by the default
  list.
- Inherited fields are merged like declared ones. A value a user set for an inherited field without
  a default used to be dropped on `update`.
- For a section, what the provider of its type returns is the section's default. A provider used to
  be unable to override an initializer value of the section.
- `@Merge` without options no longer has an effect, because it no longer names a strategy.
