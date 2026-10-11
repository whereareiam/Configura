# AGENTS.md

Configura is a set of helpers on top of Jackson for the configuration files of an application: it
loads a file into a model, fills in what the file is missing from the defaults without touching what
the user wrote, and writes the result back. YAML and JSON are built in. Java 17 or newer is required.

## Modules

- `configura-api`: what applications and features compile against. `@Merge` and its defaults,
  documents, readers, writers, exceptions and the feature contract `ConfiguraFeature`. Public API: every change is
  compatibility-sensitive and needs complete Javadocs.
- `configura-common`: the shared implementation, the merge engine, defaults, document handling,
  readers, writers and the duration format.
- `configura`: the artifact applications depend on, with the entry points such as `Config`; it
  brings the API and the implementation.
- `configura-feature/*`: one opt-in feature per module (`feature-comment`, `feature-extension`,
  `feature-polymorphic`, `feature-postprocess`), each with its own `api` module and published in the
  `me.whereareiam.configura.feature` group.
- `build-logic/`: the shared Gradle conventions (`java-conventions`, `testing`, `publish`).
- `docs/`: the guides; `docs/features/` has one page per feature.

Keep a feature's behavior in its own module. The core reaches features only through the feature
contract in `configura-api`.

## Build and test

- `./gradlew test build` builds everything and runs the tests.
- `VERSION=<version> ./gradlew publishToMavenLocal` lets an application build against a local
  version. Without `VERSION` the version is `dev`.

## Pull requests and releases

Build each change in its own `feature/...` branch and merge it into `dev` through a pull request.
Publishing a release draft tags `dev`, publishes the Maven artifacts and attaches the jars.

### Release notes

A merged pull request lands in the release draft as `* <title> ([#<number>](<url>))`, under the
category of its label. Readers are application developers.

- Titles use `Area: Description`, with a past-tense description of what changed for an application:
  `Introduced`, `Changed`, `Fixed`, `Removed`.
- Areas: `Loading` (reading, binding and writing files), `Merge` (defaults and merging), `Formats`,
  `Features` (the feature contract, write hooks and reserved keys), `Polymorphic`, `Extensions`,
  `Post-processing`, `Comments`. Add an area only for a new feature or format, and add it to this
  list in the same pull request.

| Label | Use for |
| --- | --- |
| `feature` | something new for applications |
| `change` | changed behavior or API |
| `bug` | a fix for a bug that shipped in a release |
| `dependencies` | a library update |
| `skip-changelog` | refactors, build, CI, tests and documentation, and fixes of bugs that never shipped |

Add `major` next to the category when applications must change their code on upgrade.

Text written by hand, the subtitle, the summary and upgrade notes, lives in `.github/release-notes.md`;
it replaces the draft's placeholders while its `version` matches the drafted version. The release
workflow stops while a placeholder is left.
