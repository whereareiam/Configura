## Type Adapters

Type adapters teach Configura how a declared type or type family merges.

Use them when:
- a wrapper type needs custom merge behavior
- a custom container type should participate in merge
- generic `Collection`, `Set`, or `Queue` should get intentional semantics
- a type needs special child-type traversal

### Built-in Model

Configura ships built-in adapters for:
- value fields
- object fields
- `Map`
- `List`

Built-in list behavior applies only to `List`.

### Custom Adapter

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.merge.policy.MergePolicy;
import me.whereareiam.configura.merge.type.MergeTypeAdapter;
import me.whereareiam.configura.merge.type.context.MergeTypeAdapterContext;
import me.whereareiam.configura.merge.type.descriptor.MergeTypeDescriptor;

public final class Envelope<T> {
	public T value;
}

public final class EnvelopeTypeAdapter implements MergeTypeAdapter {
	@Override
	public boolean supports(MergeTypeDescriptor descriptor, MergePolicy policy) {
		return descriptor.getDeclaredType() == Envelope.class;
	}

	@Override
	public Class<?> resolveChildType(MergeTypeDescriptor descriptor, MergePolicy policy) {
		Class<?> generic = descriptor.resolveGenericArgument(0);
		return generic != null ? generic : descriptor.getDeclaredType();
	}

	@Override
	public JsonNode merge(MergeTypeAdapterContext context) {
		JsonNode source = context.objectMember(context.getSourceNode(), "value");
		JsonNode defaults = context.objectMember(context.getDefaultNode(), "value");

		ObjectNode result = context.getMapper().createObjectNode();
		result.set("value", context.mergeChildren(source, defaults));
		return result;
	}
}
```

Register it:

```java
Config.builder()
		.mergeTypeAdapter(new EnvelopeTypeAdapter())
		.build();
```

### Override Rules

Type adapter resolution is ordered.
Later registrations override earlier ones when both claim the same field/type.
