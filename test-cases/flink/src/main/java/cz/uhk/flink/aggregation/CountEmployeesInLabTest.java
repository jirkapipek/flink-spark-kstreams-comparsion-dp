package cz.uhk.flink.aggregation;

import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.model.json.EmployeeLocation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

@Log4j2
public class CountEmployeesInLabTest {

    // Define input and output topics for Kafka
    private static final String INPUT_TOPIC = "EMPLOYEE_LOCATION";
    private static final String OUTPUT_TOPIC = "EMPLOYEE_LOCATION_AGGREGATED";
    private static final String BROKERS = "kafka.brokers";
    private static final String GROUP_ID = "kafka.groupId";

    public static void main(final String[] args) {
        try {
            buildStream();
        } catch (Exception e) {
            throw new RuntimeException(e); // Throw a runtime exception if there is an error in the stream building process.
        }
    }

    // Method to build and execute the Flink streaming application
    static void buildStream() throws Exception {
        // Load configuration settings from the ConfigurationManager
        BaseHierarchicalConfiguration config = ConfigurationManager.getInstance().getConfig();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(); // Get the execution environment.

        // Define a source for Kafka that consumes messages from the EMPLOYEE_LOCATION topic
        KafkaSource<EmployeeLocation> employeeLocationKafkaSource = KafkaSource.<EmployeeLocation>builder()
                .setBootstrapServers(config.getString(BROKERS)) // Set the address of the Kafka brokers.
                .setGroupId(config.getString(GROUP_ID)) // Set the group ID for Kafka consumer.
                .setTopics(INPUT_TOPIC) // Define the input topic.
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST)) // Start reading from the earliest offset.
                .setValueOnlyDeserializer(new JsonDeserializationSchema<>(EmployeeLocation.class)) // Deserialize JSON records into EmployeeLocation objects.
                .build();

        // Create a data stream from the Kafka source
        DataStream<EmployeeLocation> employeeLocationDataStream = env
                .fromSource(employeeLocationKafkaSource, WatermarkStrategy.noWatermarks(), "employees_location_source");

        // Key the stream by lab ID
        KeyedStream<EmployeeLocation, String> keyedStream = employeeLocationDataStream
                .keyBy(new KeySelector<EmployeeLocation, String>() { // Key selector to extract key from EmployeeLocation.
                    @Override
                    public String getKey(EmployeeLocation value) {
                        return value.lab; // Use lab field as the key.
                    }
                });

        // Count occurrences per key (lab) using a global window and a count trigger
        SingleOutputStreamOperator<Tuple2<String, Long>> labCounts = keyedStream
                .window(GlobalWindows.create()) // Use a global window.
                .trigger(CountTrigger.of(1)) // Trigger the window for every element.
                .apply(new WindowFunction<EmployeeLocation, Tuple2<String, Long>, String, GlobalWindow>() { // Custom window function to count employees.
                    @Override
                    public void apply(String key, GlobalWindow window, Iterable<EmployeeLocation> input, Collector<Tuple2<String, Long>> out) {
                        long count = 0;
                        for (EmployeeLocation in : input) {
                            count++; // Increment the count for each element in the window.
                        }
                        out.collect(new Tuple2<>(key, count)); // Output the count for this key (lab).
                    }
                });

        // Define the serialization schema for the output Kafka topic
        KafkaRecordSerializationSchema<Tuple2<String, Long>> serializationSchema = new KafkaRecordSerializationSchema<>() {
            @Nullable
            @Override
            public ProducerRecord<byte[], byte[]> serialize(Tuple2<String, Long> stringLongTuple2, KafkaSinkContext kafkaSinkContext, Long aLong) {
                String key = stringLongTuple2.f0; // Get the key (lab ID).
                String value = String.valueOf(stringLongTuple2.f1); // Convert the count to a string.
                return new ProducerRecord<>(OUTPUT_TOPIC, key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
            }
        };

        // Create and configure the Kafka sink
        KafkaSink<Tuple2<String, Long>> sinkKafka = KafkaSink.<Tuple2<String, Long>>builder()
                .setBootstrapServers(config.getString(BROKERS)) // Set the Kafka brokers.
                .setRecordSerializer(serializationSchema) // Set the serialization schema.
                .build();

        // Add the sink to the stream for outputting the results
        labCounts.sinkTo(sinkKafka);

        // Execute the Flink job
        env.execute("CountEmployeesInLab");
    }
}