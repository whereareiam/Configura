## Features

Configura features are optional modules layered on top of the core runtime.

Core functionality lives in:
- `me.whereareiam:configura`

Optional features are separate dependencies:
- `me.whereareiam.configura.feature:extension`
- `me.whereareiam.configura.feature:polymorphic`
- `me.whereareiam.configura.feature:postprocess`

### Using a feature

1. Add the dependency.
2. Wire the feature explicitly into `Config` or `Configura`.

Example:

```java
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.configura.feature.postprocess.PostProcessFeature;

Configura config = Config.builder()
		.feature(PostProcessFeature.defaults())
		.build();
```

### Feature docs

- [Extensions](features/EXTENSIONS.md)
- [Polymorphic Models](features/POLYMORPHIC.md)
- [Post-Processing](features/POST_PROCESSING.md)
