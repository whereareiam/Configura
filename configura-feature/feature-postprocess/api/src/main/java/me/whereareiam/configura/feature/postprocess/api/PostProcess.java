package me.whereareiam.configura.feature.postprocess.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be invoked after the configuration object has been loaded.
 * <p>
 * The annotated method must be public, take no parameters, and return void.
 * Multiple methods can be annotated with {@code @PostProcess}.
 *
 * <p>Example:
 * <pre>{@code
 * public class Settings {
 *     private int level;
 *     private Updater updater;
 *
 *     @PostProcess(once = true)
 *     public void initialize() {
 *         // Runs only on the first load
 *         Constants.LOG_LEVEL = this.level;
 *     }
 *
 *     @PostProcess
 *     public void onReload() {
 *         // Runs on every load/reload
 *         System.out.println("Config reloaded!");
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostProcess {
	/**
	 * When set to {@code true}, the annotated method will only be invoked
	 * on the first load of the configuration object. Subsequent loads/reloads
	 * will skip this method.
	 * <p>
	 * When set to {@code false} (default), the method is invoked on every load.
	 *
	 * @return {@code true} to run only once, {@code false} to run on every load
	 */
	boolean once() default false;
}
