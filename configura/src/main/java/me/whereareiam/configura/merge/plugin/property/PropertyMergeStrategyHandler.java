package me.whereareiam.configura.merge.plugin.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.whereareiam.configura.common.merge.strategy.FieldMergeStrategyFactory;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.MergeContext;
import me.whereareiam.configura.merge.strategy.DeepDefaults;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

@RequiredArgsConstructor
public final class PropertyMergeStrategyHandler {
	private final ObjectMapper mapper;
	private final FieldMergeStrategyFactory strategyFactory = new FieldMergeStrategyFactory();

	public @NotNull JsonNode merge(
			@NotNull Class<?> ownerType,
			Field property,
			@NotNull String propertyName,
			@NotNull Class<?> valueType,
			JsonNode sourceValue,
			JsonNode defaultValue,
			@NotNull MergeBehavior behavior,
			boolean sourceDefaultsAsMissing,
			Class<? extends FieldMergeStrategy> strategyClass,
			@NotNull MergeContext.RecursiveMerge recursiveMerge
	) {
		MergeContext context = new MergeContext(
				mapper,
				ownerType,
				property,
				propertyName,
				valueType,
				sourceValue,
				defaultValue,
				sourceDefaultsAsMissing,
				behavior,
				recursiveMerge
		);

		return strategyFactory.create(strategyClass != null ? strategyClass : DeepDefaults.class).merge(context);
	}
}
