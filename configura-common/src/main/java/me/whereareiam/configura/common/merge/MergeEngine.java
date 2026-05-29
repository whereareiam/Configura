package me.whereareiam.configura.common.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.whereareiam.configura.common.merge.defaults.MergeDefaultsProviderRegistry;
import me.whereareiam.configura.common.merge.defaults.MergeDefaultsResolver;
import me.whereareiam.configura.common.merge.resolver.MergeBehaviorResolver;
import me.whereareiam.configura.merge.MergeBehavior;
import me.whereareiam.configura.merge.plugin.MergePluginRegistry;
import me.whereareiam.configura.merge.policy.MergePolicyResolverRegistry;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategy;
import me.whereareiam.configura.merge.strategy.FieldMergeStrategyRegistry;

public final class MergeEngine {
	private final ObjectMapper mapper;
	private final MergeDefaultsResolver defaultsResolver;
	private final MergeBehaviorResolver behaviorResolver = new MergeBehaviorResolver();
	private final MergeCoordinator mergeCoordinator;
	private final MergeBehavior behavior;

	public MergeEngine(
			ObjectMapper mapper,
			MergeDefaultsProviderRegistry defaultsRegistry,
			FieldMergeStrategyRegistry strategyRegistry,
			MergePluginRegistry pluginRegistry,
			MergePolicyResolverRegistry policyResolverRegistry,
			Class<? extends FieldMergeStrategy> defaultStrategy,
			MergeBehavior behavior
	) {
		this.mapper = mapper;
		this.defaultsResolver = new MergeDefaultsResolver(mapper, defaultsRegistry, pluginRegistry, policyResolverRegistry);
		this.mergeCoordinator = new MergeCoordinator(
				mapper,
				strategyRegistry,
				defaultStrategy,
				defaultsResolver,
				pluginRegistry,
				policyResolverRegistry
		);
		this.behavior = behavior != null ? behavior : MergeBehavior.defaults();
	}

	public <T> ObjectNode defaultsNode(T model, Class<T> type) {
		return defaultsNodeInternal(model, type, MergeOperation.userModel());
	}

	public <T> ObjectNode mergeUserModel(JsonNode source, T model, Class<T> type) {
		return mergeInternal(source, model, type, MergeOperation.userModel());
	}

	public <T> ObjectNode mergeDefaults(JsonNode source, T model, Class<T> type) {
		return mergeInternal(source, model, type, MergeOperation.syntheticDefaults(behavior));
	}

	private <T> ObjectNode defaultsNodeInternal(T model, Class<T> type, MergeOperation operation) {
		return defaultsResolver.resolve(model, type, operation.defaultsPolicy());
	}

	private <T> ObjectNode mergeInternal(JsonNode source, T model, Class<T> type, MergeOperation operation) {
		ObjectNode defaults = defaultsNodeInternal(model, type, operation);
		JsonNode merged = mergeCoordinator.mergeObject(
				source,
				defaults,
				type,
				false,
				behaviorResolver.resolve(behavior, type),
				operation
		);
		return merged instanceof ObjectNode objectNode ? objectNode : mapper.createObjectNode();
	}
}
