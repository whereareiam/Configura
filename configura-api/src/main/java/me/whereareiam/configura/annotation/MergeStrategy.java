package me.whereareiam.configura.annotation;

/**
 * Defines how template defaults should be merged with existing user configuration.
 */
public enum MergeStrategy {
    /**
     * Deep merge - recursively merge nested structures.
     * Template adds missing keys while preserving user changes.
     *
     * <p>Example: If template has {a:1, b:2} and user has {a:5}, result is {a:5, b:2}</p>
     *
     * <p>Use for nested config objects where you want to add new template keys.</p>
     */
    DEEP,

    /**
     * Shallow merge - only apply template if field is completely missing.
     * Once user has any value (even empty), template is ignored.
     *
     * <p>Example scenarios:</p>
     * <ul>
     *   <li>User file missing field → Apply full template</li>
     *   <li>User has {a:5} → Keep {a:5}, don't add template's b:2</li>
     *   <li>User has {} → Keep {}, don't add anything from template</li>
     * </ul>
     *
     * <p>Use for Maps/Lists where users should control all entries.</p>
     */
    SHALLOW,

    /**
     * No merge - template is never applied.
     *
     * <p>Field is either from user config or omitted. No template defaults are applied.</p>
     *
     * <p>Use for pure user data with no template defaults.</p>
     */
    NONE
}
