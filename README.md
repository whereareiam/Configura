# Configura

Configura provides convenient Java configuration on top of Jackson, with
model defaults, merge policies, hooks, and optional features. Java 17 or newer
is required. YAML and JSON are supported out of the box.

## Installation

```kotlin
repositories {
    maven("https://registry.whereareiam.me/maven/packages")
}

dependencies {
    implementation("me.whereareiam:configura:1.0.0")
}
```

Optional features are separate artifacts under `me.whereareiam.configura.feature`:
`extension`, `polymorphic`, and `postprocess`. Use the same version as Configura.

## Usage

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.annotation.merge.MergeValue;

public class Settings {
    @MergeValue(text = "world")
    public String name;
}
```

```java
var yaml = Config.yaml();
Settings settings = yaml.update("config/settings", Settings.class);
```

`update` creates a missing file, merges defaults, validates binding, and atomically
replaces the destination. If binding or serialization fails, the existing file is
preserved. The filesystem must support atomic replacement.

Use `read` without rewriting, `write` for an exact model write, and `save` when
applying the configured model merge behavior. For JSON, use `Config.json()`.

```java
var yaml = Config.builder()
        .format(Format.YAML)
        .feature(PostProcessFeature.defaults())
        .build();
```

`DefaultsProvider`, merge annotations and custom `MergeTypeAdapter` implementations
let applications supply defaults and extend merging. Features add document type
resolution, extensions and post-binding processing without changing basic loading.

For staged work, `prepareNode(tree, Settings.class)` applies defaults and binding
hooks entirely in memory. `readNode` and `writeNodeBytes` expose raw document trees
and serialization without forcing old documents into current Java models.

## Migrations

Installation upgrades belong to [Strata](https://github.com/whereareiam/strata).
Use `strata-integration-configura` to move values between files, split or merge
documents, validate prepared output, and recover interrupted multi-file commits.
Run Strata before normal configuration updates.

This is a breaking migration-API change: `versioned`, `withVersioned`, migration
steps, and automatic migration backups have been removed. Port transformations to
Strata actions and register explicit legacy detectors. Configura no longer executes
version chains during reads or updates.

`ConfigDocument` and `@DocumentVersion` remain as portable metadata. Configura
preserves the supplied version; it does not infer, advance, or validate migration
versions. Strata owns upgrade history and coordinated backups.

## Building and publishing

```sh
./gradlew test build
```

Development publishing runs only through the manual workflow. Releases run tests,
publish to `registry.whereareiam.me/maven/packages` through the shared DevOps OIDC
action, and attach binary, source and Javadoc JARs. Registry credentials are not
needed for normal local builds.

Release Drafter follows `dev`. Use `feature`, `change`, `bug`, or `dependencies`
labels; add `major` for breaking changes and `skip-changelog` to omit an entry.
