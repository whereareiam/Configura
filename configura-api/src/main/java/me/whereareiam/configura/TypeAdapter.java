package me.whereareiam.configura;

import me.whereareiam.configura.node.Node;
import me.whereareiam.configura.node.NullNode;
import me.whereareiam.configura.node.StringNode;

/**
 * Adapter for custom type serialization and deserialization.
 *
 * @param <T> the type to adapt
 */
public interface TypeAdapter<T> {
	/**
	 * Deserializes a string value to the target type.
	 *
	 * @param value the string value from config file
	 * @return the deserialized object
	 * @throws Exception if deserialization fails
	 */
	T deserialize(String value) throws Exception;

	/**
	 * Serializes an object to a string.
	 *
	 * @param value the object to serialize
	 * @return the string representation
	 * @throws Exception if serialization fails
	 */
	String serialize(T value) throws Exception;

	/**
	 * Deserializes a structured node to the target type.
	 *
	 * @param node the value node
	 * @return the deserialized object
	 * @throws Exception if deserialization fails
	 */
	default T deserializeNode(Node node) throws Exception {
		return deserialize(node != null ? node.asText() : null);
	}

	/**
	 * Serializes an object to a structured node.
	 *
	 * @param value the object to serialize
	 * @return the node representation
	 * @throws Exception if serialization fails
	 */
	default Node serializeNode(T value) throws Exception {
		String raw = serialize(value);
		return raw != null ? new StringNode(raw) : NullNode.instance();
	}
}


