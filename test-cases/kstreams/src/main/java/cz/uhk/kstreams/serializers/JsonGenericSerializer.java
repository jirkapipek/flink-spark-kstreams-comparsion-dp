package cz.uhk.kstreams.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

/**
 * A generic JSON serializer implementation for Kafka Streams.
 * This serializer serializes Java objects into JSON format.
 *
 * @param <T> The type of Java objects to serialize to JSON.
 */
public class JsonGenericSerializer<T> implements Serializer<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Serializes a Java object into JSON format.
     *
     * @param topic The topic associated with the data.
     * @param data  The Java object to be serialized.
     * @return The byte array representing the serialized JSON data.
     * @throws SerializationException If an error occurs during serialization.
     */
    @Override
    public byte[] serialize(String topic, T data) throws SerializationException {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}