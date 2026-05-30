package me.whereareiam.configura.feature.extension.api;

import lombok.Getter;
import me.whereareiam.configura.document.DocumentTypeContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Declares one extension rule that may resolve a declared base document type to a subtype.
 *
 * @param <T> declared base document type
 */
public final class ConfigDocumentRule<T> {
	private final @NotNull Class<T> baseType;
	private final @NotNull Class<? extends T> targetType;
	private final @NotNull Predicate<DocumentTypeContext> selector;
	@Getter
	private final boolean wholeDocument;

	private ConfigDocumentRule(
			@NotNull Class<T> baseType,
			@NotNull Class<? extends T> targetType,
			@NotNull Predicate<DocumentTypeContext> selector,
			boolean wholeDocument
	) {
		this.baseType = Objects.requireNonNull(baseType, "baseType");
		this.targetType = Objects.requireNonNull(targetType, "targetType");
		this.selector = Objects.requireNonNull(selector, "selector");
		this.wholeDocument = wholeDocument;
		if (!baseType.isAssignableFrom(targetType))
			throw new IllegalArgumentException("Target type " + targetType.getName() + " must extend " + baseType.getName());
	}

	/**
	 * Creates a whole-document rule that applies to every extendable occurrence of the base type.
	 *
	 * @param baseType declared base document type
	 * @param targetType target subtype to use
	 * @param <T> declared base document type
	 * @return new whole-document rule
	 */
	public static <T> @NotNull ConfigDocumentRule<T> whole(
			@NotNull Class<T> baseType,
			@NotNull Class<? extends T> targetType
	) {
		return new ConfigDocumentRule<>(baseType, targetType, context -> true, true);
	}

	/**
	 * Creates a contextual document rule.
	 *
	 * @param baseType declared base document type
	 * @param targetType target subtype to use when the predicate matches
	 * @param selector predicate evaluated against the current document context
	 * @param <T> declared base document type
	 * @return new contextual document rule
	 */
	public static <T> @NotNull ConfigDocumentRule<T> when(
			@NotNull Class<T> baseType,
			@NotNull Class<? extends T> targetType,
			@NotNull Predicate<DocumentTypeContext> selector
	) {
		return new ConfigDocumentRule<>(baseType, targetType, selector, false);
	}

	/**
	 * Returns the declared base document type for this rule.
	 *
	 * @return declared base type
	 */
	public @NotNull Class<T> getBaseType() {
		return baseType;
	}

	/**
	 * Returns the subtype that should be used when this rule applies.
	 *
	 * @return target subtype
	 */
	public @NotNull Class<? extends T> getTargetType() {
		return targetType;
	}

	/**
	 * Returns the contextual predicate used to decide whether this rule applies.
	 *
	 * @return rule predicate
	 */
	public @NotNull Predicate<DocumentTypeContext> getSelector() {
		return selector;
	}
}
