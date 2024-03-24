package cz.uhk.flink.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.model.json.Employee;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Log4j2
public class FilterEmployeesTest {

    // Kafka topics for input and output
    static final String INPUT_TOPIC = "EMPLOYEES";
    static final String OUTPUT_TOPIC = "EMPLOYEES_FILTERED";

    // Configuration parameters for Kafka brokers and consumer group ID
    private static final String BROKERS = "kafka.brokers";
    private static final String GROUP_ID = "kafka.groupId";

    public static void main(final String[] args) {
        try {
            buildStream();
        } catch (Exception e) {
            throw new RuntimeException(e);  // Handling any exception that might occur during the stream building and execution.
        }
    }

    // Main method to build and execute the Flink stream processing
    static void buildStream() throws Exception {
        // Load configuration settings from an external source or configuration file
        BaseHierarchicalConfiguration config = ConfigurationManager.getInstance().getConfig();

        // Initialize the Flink streaming execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Define a Kafka source to consume messages from the input topic with a custom deserializer
        KafkaSource<Tuple2<String, Employee>> employeeLocationKafkaSource = KafkaSource.<Tuple2<String, Employee>>builder()
                .setBootstrapServers(config.getString(BROKERS))  // Kafka broker configuration
                .setGroupId(config.getString(GROUP_ID))          // Consumer group ID for Kafka
                .setTopics(INPUT_TOPIC)                          // Set the input topic name
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))  // Start reading from the earliest offset
                .setDeserializer(new KafkaRecordDeserializationSchema<>() {
                    // Custom deserialization schema to convert Kafka records into Tuple2<String, Employee> objects
                    @Override
                    public TypeInformation<Tuple2<String, Employee>> getProducedType() {
                        return TypeInformation.of(new TypeHint<>() {
                        });
                    }

                    @Override
                    public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord, Collector<Tuple2<String, Employee>> collector) throws IOException {
                        // Extract the key (a string) and value (an employee object) from each Kafka record
                        String key = new String(consumerRecord.key());
                        Employee value = null;

                        if (consumerRecord.value() != null) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            value = objectMapper.readValue(consumerRecord.value(), Employee.class);
                        }

                        collector.collect(new Tuple2<>(key, value));
                    }
                })
                .build();

        // Create a data stream by consuming messages from the source
        DataStream<Tuple2<String, Employee>> employeeDataStream = env
                .fromSource(employeeLocationKafkaSource, WatermarkStrategy.noWatermarks(), "employees_source");

        // Filter the data stream to include only female employees with an hourly rate greater than 15
        DataStream<Tuple2<String, Employee>> filterDataStream = employeeDataStream.filter(employeeTuple ->
                "female".equals(employeeTuple.f1.gender) && employeeTuple.f1.hourlyRate > 15);

        // Define a serialization schema for sending the filtered data to the output Kafka topic
        KafkaRecordSerializationSchema<Tuple2<String, Employee>> serializationSchema = new KafkaRecordSerializationSchema<>() {
            @Override
            public ProducerRecord<byte[], byte[]> serialize(Tuple2<String, Employee> stringEmployeeTuple2, KafkaSinkContext kafkaSinkContext, Long aLong) {
                // Serialize the Tuple2 object to a ProducerRecord to be sent to the Kafka topic
                ObjectMapper objectMapper = new ObjectMapper();
                String key = stringEmployeeTuple2.f0;
                String value = null;
                try {
                    value = objectMapper.writeValueAsString(stringEmployeeTuple2.f1);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);  // Error handling for JSON processing exceptions
                }

                return new ProducerRecord<>(OUTPUT_TOPIC, key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
            }
        };

        // Create a Kafka sink to produce messages to the output topic
        KafkaSink<Tuple2<String, Employee>> sinkKafka = KafkaSink.<Tuple2<String, Employee>>builder()
                .setBootstrapServers(config.getString(BROKERS))  // Kafka broker configuration
                .setRecordSerializer(serializationSchema)        // Set the serialization schema for the sink
                .build();

        // Connect the filtered data stream to the Kafka sink
        filterDataStream.sinkTo(sinkKafka);

        // Execute the Flink job
        env.execute("FilterEmployees");
    }
}
