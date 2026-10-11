# Features

A feature adds behaviour to a `Configura` instance. You register it on the builder; nothing is
picked up automatically.

```java
Configura configura = Configura.builder()
		.feature(PolymorphicFeature.defaults())
		.feature(PostProcessFeature.defaults())
		.build();
```

## Shipped features

Each is its own dependency under `me.whereareiam.configura.feature`, with the same version as
Configura.

| Feature | Artifact | Use it to |
| --- | --- | --- |
| [Polymorphic models](features/POLYMORPHIC.md) | `polymorphic` | Bind a base type to a subtype chosen by a discriminator value or by which fields are present. |
| [Extensions](features/EXTENSIONS.md) | `extension` | Let another module replace a document type with its own subtype. |
| [Post-processing](features/POST_PROCESSING.md) | `postprocess` | Run a method of the model after it has been loaded, for derived values or validation. |
| [Comments](features/COMMENTS.md) | `comment` | Write comments declared on the model into the YAML and JSON files Configura writes. |

## Writing a feature

Implement `ConfiguraFeature` from `configura-api`. Every method has an empty default, so a feature
overrides only what it contributes.

| Method | Contribution |
| --- | --- |
| `modules(plainMapper)` | Jackson modules to register on the mapper. |
| `typeResolvers()` | `DocumentTypeResolver`s, which pick a more specific model type for a place in the document. They are asked while defaults are built, while the file is merged and while it is bound. |
| `phases()` | `DocumentPhase`s, called with every model after it has been bound. |
| `reservedKeys()` | Top-level keys of a document that belong to the feature. |
| `writeHooks()` | `DocumentWriteHook`s, which choose the generator a document is written through, to add to or change what is written. |

### Write hooks

A write hook is asked for the `JsonGenerator` each document is written through. It gets a
`DocumentWrite`: the content, the model type (`null` for `writeNode`), the mapper and the output
stream. Hooks form a chain in registration order: a hook usually calls `next.open()` and returns that
generator, wrapped if it wants to see or add to what is written; it may also open its own generator on
the output, and later hooks are then not asked. Without hooks, documents are written exactly as before.

```java
public final class AuditFeature implements ConfiguraFeature {
	@Override
	public @NotNull List<DocumentWriteHook> writeHooks() {
		return List.of((write, next) -> {
			log.info("Writing {}", write.getModelType());
			return next.open();
		});
	}
}
```

### Reserved keys

A reserved key is not part of the model. Configura does not bind it, and `update`, `save` and
`write` carry its value over from the existing file to the top of the new content instead of
dropping it as an unknown key. A tool that keeps its own marker in a configuration file uses this;
Strata's `StrataFeature` reserves `_version` that way.

```java
public final class MarkerFeature implements ConfiguraFeature {
	@Override
	public @NotNull Set<String> reservedKeys() {
		return Set.of("_marker");
	}
}
```

Configura never writes a reserved key on its own. The owner of the key writes it, for example
through `readNode` and `writeNode`; Configura then keeps it through every rewrite of the file.
`configura.reservedKeys()` lists the keys of all registered features.
