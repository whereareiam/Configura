package me.whereareiam.configura.merge.strategy.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.strategy.MergeStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Keeps only keys declared by defaults while recursively filling missing values for those keys.
 */
public final class DefaultKeysOnlyMap implements MergeStrategy {
	@Override
	public @Nullable JsonNode merge(@NotNull MergeContext context) {
		JsonNode source = context.getSourceNode();
		JsonNode defaults = context.getDefaultNode();
		if (source == null || source.isNull() || context.sourceTreatsDefaultAsMissing())
			return defaults == null ? null : defaults.deepCopy();
		if (!source.isObject() || defaults == null || !defaults.isObject())
			return source.deepCopy();

		JsonNode merged = context.mergeChildren(source, defaults);
		ObjectNode result = context.getMapper().createObjectNode();
		Iterator<String> defaultKeys = defaults.fieldNames();
		while (defaultKeys.hasNext()) {
			String key = defaultKeys.next();
			JsonNode value = merged.get(key);
			if (value != null)
				result.set(key, value.deepCopy());
		}

		return result;
	}
}
