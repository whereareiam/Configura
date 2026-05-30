## Merge Overview

Merging is how Configura turns:
- source data from a file
- defaults for missing values
- field-specific merge rules

into one resolved document.

The practical model is:
- every field has a shape
- a strategy decides how source and defaults interact for that field
- defaults provide seed data for the shape
- a type adapter teaches Configura how that type family should merge

Most fields use Configura's built-in shapes and built-in adapters.
Custom or wrapper types can opt into custom merge behavior through `MergeTypeAdapter`.

### Core Shapes

Configura treats these as core merge shapes:
- value
- object
- map
- list

Built-in list semantics apply only to `List`.
`Collection`, `Set`, `Queue`, and custom container types must be taught explicitly through a type adapter.

### How a Field Is Resolved

When Configura resolves a field, it combines:
- a merge strategy
- defaults for the field
- a type adapter for the field shape or type family

At runtime:
1. a merge policy resolver selects a strategy
2. a defaults resolver produces seed data
3. a type adapter merges source data and defaults for that field shape/type

### Built-in DSL

The built-in merge/defaults DSL lives under `me.whereareiam.configura.annotation.merge`.

Main annotations:
- `@Merge`
- `@MergeValue`
- `@MergeObject`
- `@MergeMap`
- `@MergeList`
- `@MergeDefaultsSource`
- `@DefaultsProvider`

### Related Guides

- [Strategies](merge/STRATEGIES.md)
- [Defaults Resolution](merge/DEFAULTS.md)
- [Type Adapters](merge/TYPE_ADAPTERS.md)
- [Annotation DSL](merge/ANNOTATIONS.md)
