package me.whereareiam.configura.feature.comment.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Writes comment lines above a setting whenever Configura writes the document. Takes effect when the
 * Configura instance has the {@code CommentFeature} registered; without it the annotation is ignored.
 *
 * <p>On a field, the lines are written above that field's key, wherever the field's class appears in
 * the document: in a section, in every entry of a map or list, and in polymorphic subtypes. On the
 * class of a document, they are written at the top of the file.</p>
 *
 * <pre>{@code
 * @Comment("Settings of the lobby server.")
 * public class Settings {
 * 	@Comment({"Seconds before an idle player is moved.", "0 disables it."})
 * 	public int timeout = 30;
 * }
 * }</pre>
 *
 * <p>YAML writes the lines with {@code #}, JSON with {@code //}, which Configura reads back. JSON
 * written without indentation, and other formats registered through
 * {@code Configura.Builder#format(String, java.util.function.Function)}, get no comments. An empty
 * string writes an empty comment line. Comments are taken from the model on every
 * write; comments a user adds to the file by hand are not kept.</p>
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Comment {
	/**
	 * Returns the comment, one element per line, without the comment marker.
	 *
	 * @return comment lines
	 */
	String[] value();
}
