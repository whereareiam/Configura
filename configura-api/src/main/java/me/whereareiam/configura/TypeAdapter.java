package me.whereareiam.configura;

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
}


