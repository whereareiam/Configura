package me.whereareiam.configura;

import me.whereareiam.configura.type.Format;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Holds the {@link Configura} an application shares, for code that cannot have it handed in, and
 * offers shortcuts for creating one.
 */
public final class Config {
	private static Configura configured = yaml();

	private Config() {
	}

	/**
	 * Returns the shared instance: the one last passed to {@link #setConfigured(Configura)}, or a
	 * plain YAML instance before that.
	 *
	 * @return shared instance
	 */
	public static @NotNull Configura configured() {
		return configured;
	}

	/**
	 * Replaces the shared instance.
	 *
	 * @param configura instance the application was set up with
	 */
	public static void setConfigured(@NotNull Configura configura) {
		configured = Objects.requireNonNull(configura, "configura");
	}

	/**
	 * Starts a builder with the built-in merge rules and the YAML format. The shared instance has no
	 * influence on it.
	 *
	 * @return new builder
	 */
	public static @NotNull Configura.Builder builder() {
		return Configura.builder();
	}

	/**
	 * Creates a plain instance for YAML files.
	 *
	 * @return new instance
	 */
	public static @NotNull Configura yaml() {
		return Configura.builder().format(Format.YAML).build();
	}

	/**
	 * Creates a plain instance for JSON files.
	 *
	 * @return new instance
	 */
	public static @NotNull Configura json() {
		return Configura.builder().format(Format.JSON).build();
	}
}
