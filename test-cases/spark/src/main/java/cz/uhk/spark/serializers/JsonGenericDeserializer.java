package cz.uhk.spark.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * A generic JSON deserializer implementation for Kafka Streams.
 * This deserializer deserializes JSON messages into Java objects of a specified class.
 *
 * @param <T> The type of Java objects to deserialize JSON into.
 */
public class JsonGenericDeserializer<T> implements Deserializer<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Class<T> deserializedClass;

    /**
     * Constructs a new JsonGenericDeserializer instance with the specified deserialized class.
     *
     * @param deserializedClass The class of Java objects to deserialize JSON into.
     */
    public JsonGenericDeserializer(Class<T> deserializedClass) {
        this.deserializedClass = deserializedClass;
    }

    /**
     * Deserializes JSON data into a Java object of the specified class.
     *
     * @param topic The topic associated with the data.
     * @param data  The byte array representing the serialized JSON data.
     * @return The deserialized Java object.
     * @throws SerializationException If an error occurs during deserialization.
     */
    @Override
    public T deserialize(String topic, byte[] data) throws SerializationException {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.readValue(data, deserializedClass);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing JSON message", e);
        }
    }
}
