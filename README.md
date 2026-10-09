# Configura

Configura is a set of helpers on top of Jackson for the configuration files of an application:
it loads a file into your model, fills in what the file is missing from your defaults without
touching what the user wrote, and writes the result back. Jackson stays visible: you register your
own modules and can reach the `ObjectMapper`. YAML and JSON are built in. Java 17 or newer is
required.

## Installation

```kotlin
repositories {
    maven("https://registry.whereareiam.me/maven/packages")
}

dependencies {
    implementation("me.whereareiam:configura:2.0.0")
}
```

## Load a configuration file

```java
import me.whereareiam.configura.Config;

public class Settings {
    public String name = "lobby";
    public int timeout = 30;
}

Settings settings = Config.yaml().update("config/settings", Settings.class);
```

`update` reads `config/settings.yml`, adds every setting the file does not have yet, binds the
result to `Settings` and writes the file back. A missing file is created with the defaults. A user
who sets `timeout: 60` keeps it; a setting you add in a later release appears in their file with
its default on the next start.

The file is replaced atomically. If the content cannot be bound to the model, `update` throws a
`ConfigException` and leaves the file as it was.

## Where to go next

- [Getting started](docs/GETTING_STARTED.md): your own `Configura` instance, defaults providers,
  Jackson modules, the other read and write methods.
- [Defaults and merging](docs/MERGE.md): where defaults come from and how `@Merge` decides what is
  added to a user's file.
- [Features](docs/FEATURES.md): polymorphic models, extendable documents, post-processing, and
  writing your own.
- [Upgrading from 1.0](docs/UPGRADING.md): what changed in the API and what to replace it with.

To rename or move settings between releases, use
[Strata](https://github.com/whereareiam/strata) and run it before `update`.

## Building

```sh
./gradlew test build
```
