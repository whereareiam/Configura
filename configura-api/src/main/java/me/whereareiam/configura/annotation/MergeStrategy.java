package me.whereareiam.configura.annotation;

/**
 * Defines how template defaults should be merged with existing user configuration.
 *
 * <p>This enum controls the behavior when merging template-provided defaults with
 * user configuration files, particularly for collection types like Maps and Lists.</p>
 */
public enum MergeStrategy {
    /**
     * Default deep merge behavior.
     *
     * <p>Recursively merges all missing keys from template into existing config.
     * For Maps/Objects: adds any keys present in template but missing in user config.
     * This means deleted entries will reappear from templates.</p>
     *
     * <p>Example: If template has {a:1, b:2} and user has {a:5}, result is {a:5, b:2}</p>
     */
    DEFAULT,

    /**
     * Map additive-only strategy (recommended for user-editable Maps).
     *
     * <p>Only applies template defaults if the field itself is missing/null in user config.
     * Once user has the field (even if empty), no keys are added from template.
     * This allows users to permanently delete unwanted template entries.</p>
     *
     * <p>Example scenarios:</p>
     * <ul>
     *   <li>User file missing field → Apply full template Map</li>
     *   <li>User has {a:5} → Keep {a:5}, don't add template's b:2</li>
     *   <li>User has {} → Keep {}, don't add anything from template</li>
     * </ul>
     *
     * <p>Use this for Maps where users should be able to remove entries (e.g., languages, commands).</p>
     */
    MAP_ADDITIVE_ONLY,

    /**
     * Skip merging entirely.
     *
     * <p>Field is either from user config or omitted. No template defaults are applied.</p>
     */
    SKIP
}
