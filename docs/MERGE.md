# Defaults and merging

`update` reads a file, completes it with the defaults of your model, binds the result and writes it
back. What the user wrote always wins; the rules on this page decide what is added around it.

## Where defaults come from

**Field initializers.**

```java
public class Settings {
	public String name = "lobby";
	public int timeout = 30;
	public Retry retry;                      // a section: takes the defaults of Retry
}

public class Retry {
	public int attempts = 3;
}
```

A section left null takes the defaults of its own type, so `retry.attempts` is 3 without an
initializer on `retry`.

**A provider**, when defaults need code or live apart from the model.

```java
public final class SettingsDefaults implements DefaultsProvider<Settings> {
	@Override
	public Settings supply(Settings settings) {
		settings.name = "lobby";
		settings.servers = List.of(server("hub"), server("arena"));
		return settings;
	}
}

Configura configura = Configura.builder()
		.defaults(SettingsDefaults.class)
		.build();
```

A provider receives a freshly constructed model and returns it filled in. Providers of superclasses
run first. At the root of a document a provider fills what the initializers left unset; for a
section, what the provider of its type returns is that section's default.

Java cannot tell an `int` or `boolean` that was never set from one set to `0` or `false`. Where two
defaults meet (a provider and an initializer, or a section's value and its type's defaults), a `0`,
a `false` or an empty list therefore counts as unset. `MergeBehavior.builder()
.primitiveDefaults(PrimitiveDefaultPolicy.PRESERVE)` turns that off. This only concerns defaults:
a `0`, `false` or `[]` a user writes in the file is always kept.

## What happens to a field

Without an annotation:

| The file | Result |
| --- | --- |
| lacks the field | the default is written |
| has a value | the user's value is kept |
| has a section | the user's values are kept and missing ones are filled in from the default |
| has a list | the user's list is taken as a whole |
| has a map | the user's entries are kept and default entries they lack are added |
| has `null` | `null` is kept |
| has a key the model does not know | it is dropped |

`@Merge` changes this per field:

```java
public class Providers {
	@Merge(absent = WhenAbsent.LEAVE_ABSENT)
	public Session session;

	@Merge(present = WhenPresent.KEEP_AS_WRITTEN)
	public Layout layout;

	@Merge(key = "id", missingEntries = MissingEntries.OMIT)
	public List<ProviderEntry> providers;

	@Merge(unknownEntries = UnknownEntries.REJECT)
	public Map<String, Listener> listeners;
}
```

| Option | Values | Effect |
| --- | --- | --- |
| `absent` | `ADD_DEFAULT` (default), `LEAVE_ABSENT` | Whether a field the file lacks is written. `LEAVE_ABSENT` keeps an optional section out of the file until the user adds it; from then on it is filled like any other. |
| `present` | `FILL_MISSING` (default), `KEEP_AS_WRITTEN` | Whether what the user left out below a field they wrote is filled in. `KEEP_AS_WRITTEN` takes a section, list or map exactly as it is in the file. |
| `key` | property name | Makes a list of objects keyed: entries of the file and of the default are matched by this property and merged one by one. Entries without it, or two with the same value, fail the load. |
| `missingEntries` | `ADD` (default), `OMIT` | For a map or keyed list the file has: whether default entries the user does not have are added. When the file has no such map or list at all, the whole default is used either way. |
| `unknownEntries` | `ALLOW` (default), `REJECT` | For a map or keyed list: whether entries the default does not have are accepted. |

Each entry of a map or keyed list is completed from the default entry with the same key and from the
defaults of the entry's type, so an entry the user adds on their own still gets its type's defaults.

`@PreserveUnknownFields` on a class or a field keeps keys the model does not know, below that point.
`MergeBehavior.builder().unknownFields(UnknownFieldPolicy.PRESERVE)` does so for a whole instance.

The rules describe the file. A model's field initializers still run when Jackson constructs it, so
after `KEEP_AS_WRITTEN` on `layout: {}` the file stays `{}` while the bound `Layout` has its
initializer values. Use a provider for defaults that must not appear on a bound object unasked.

## Which method merges

| Method | Reads the file | Applies defaults | Writes |
| --- | --- | --- | --- |
| `update(path, type)` | yes | yes | yes |
| `merge(path, model)` | yes | yes, with the model's values as defaults | no |
| `save(path, model)` | only for keys a feature reserves | yes, into the model's values | yes |
| `read(path, type)` | yes | no | no |
| `write(path, model)` | no | no | yes |
