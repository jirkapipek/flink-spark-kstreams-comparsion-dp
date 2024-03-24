package cz.uhk.flink.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.model.json.Employee;
import cz.uhk.model.json.EmployeeLocation;
import cz.uhk.model.json.EnrichedEmployee;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Log4j2
public class EnrichEmployeesTest {

    static final String INPUT_TOPIC = "EMPLOYEES"; // Input Kafka topic for employee data
    static final String INPUT_LOCATION_TOPIC = "EMPLOYEE_LOCATION"; // Input Kafka topic for employee location data
    static final String OUTPUT_TOPIC = "EMPLOYEES_ENRICHED"; // Output Kafka topic for enriched employee data
    private static final String BROKERS = "kafka.brokers"; // Kafka brokers configuration key
    private static final String GROUP_ID = "kafka.groupId"; // Kafka group ID configuration key

    public static void main(final String[] args) throws ConfigurationException, IOException {
        // Main method to handle the stream processing application execution
        try {
            buildStream();
        } catch (Exception e) {
            throw new RuntimeException(e);  // Handling any exception that might occur during the stream building and execution.
        }
    }

    // Method to build and execute the stream processing logic
    static void buildStream() throws Exception {

        BaseHierarchicalConfiguration config = ConfigurationManager.getInstance().getConfig();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // Define source Kafka topic for Employee data
        KafkaSource<Tuple2<String, Employee>> employeeKafkaSource = KafkaSource.<Tuple2<String, Employee>>builder()
                .setBootstrapServers(config.getString(BROKERS))
                .setGroupId(config.getString(GROUP_ID))
                .setTopics(INPUT_TOPIC)
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setDeserializer(new KafkaRecordDeserializationSchema<>() {
                    // Define how to deserialize Kafka records into Employee tuples
                    @Override
                    public TypeInformation<Tuple2<String, Employee>> getProducedType() {
                        return TypeInformation.of(new TypeHint<>() {
                        });
                    }

                    @Override
                    public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord, Collector<Tuple2<String, Employee>> collector) throws IOException {
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
        // Similar Kafka source configuration for EmployeeLocation data.

        KafkaSource<Tuple2<String, EmployeeLocation>> employeeLocationKafkaSource = KafkaSource.<Tuple2<String, EmployeeLocation>>builder()
                .setBootstrapServers(config.getString(BROKERS))
                .setGroupId(config.getString(GROUP_ID))
                .setTopics(INPUT_LOCATION_TOPIC)
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setDeserializer(new KafkaRecordDeserializationSchema<>() {
                    // Define how to deserialize Kafka records into Employee tuples
                    @Override
                    public TypeInformation<Tuple2<String, EmployeeLocation>> getProducedType() {
                        return TypeInformation.of(new TypeHint<>() {
                        });
                    }

                    @Override
                    public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord, Collector<Tuple2<String, EmployeeLocation>> collector) throws IOException {
                        String key = new String(consumerRecord.key());
                        EmployeeLocation value = null;

                        if (consumerRecord.value() != null) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            value = objectMapper.readValue(consumerRecord.value(), EmployeeLocation.class);
                        }

                        collector.collect(new Tuple2<>(key, value));
                    }
                })
                .build();

        // Create a data stream from the source
        DataStream<Tuple2<String, Employee>> employeeDataStream = env
                .fromSource(employeeKafkaSource, WatermarkStrategy.noWatermarks(), "employees_source");

        // Broadcast state descriptor for storing Employee information.
        final MapStateDescriptor<String, Employee> employeeStateDescriptor = new MapStateDescriptor<>(
                "employees",
                BasicTypeInfo.STRING_TYPE_INFO,
                TypeInformation.of(new TypeHint<Employee>() {
                }));

        // Creating a data stream for EmployeeLocation from the Kafka source.
        DataStream<Tuple2<String, EmployeeLocation>> employeeLocationDataStream = env
                .fromSource(employeeLocationKafkaSource, WatermarkStrategy.noWatermarks(), "employees_location_source");

        // Broadcasting Employee data stream for joining with EmployeeLocation data.
        BroadcastStream<Tuple2<String, Employee>> broadcastEmployeeStream = employeeDataStream.broadcast(employeeStateDescriptor);

        // Joining EmployeeLocation data with the broadcasted Employee data.
        DataStream<Tuple2<String, EnrichedEmployee>> enriched = // your location stream
                employeeLocationDataStream.keyBy(location -> location.f0)
                        .connect(broadcastEmployeeStream)
                        .process(new KeyedBroadcastProcessFunction<String, Tuple2<String, EmployeeLocation>, Tuple2<String, Employee>, Tuple2<String, EnrichedEmployee>>() {

                            @Override
                            public void processElement(Tuple2<String, EmployeeLocation> stringEmployeeLocationTuple2, KeyedBroadcastProcessFunction<String, Tuple2<String, EmployeeLocation>, Tuple2<String, Employee>, Tuple2<String, EnrichedEmployee>>.ReadOnlyContext readOnlyContext, Collector<Tuple2<String, EnrichedEmployee>> collector) throws Exception {
                                Employee employee = readOnlyContext.getBroadcastState(employeeStateDescriptor).get(stringEmployeeLocationTuple2.f0);
                                if (employee != null) {
                                    // If Employee data is found, enrich and emit the EnrichedEmployee object
                                    EnrichedEmployee enrichedEmployee = new EnrichedEmployee();
                                    enrichedEmployee.employeeId = stringEmployeeLocationTuple2.f1.employeeId;
                                    enrichedEmployee.firstName = employee.firstName;
                                    enrichedEmployee.lastName = employee.lastName;
                                    enrichedEmployee.lab = stringEmployeeLocationTuple2.f1.lab;

                                    collector.collect(new Tuple2<>(String.valueOf(stringEmployeeLocationTuple2.f1.employeeId),enrichedEmployee));
                                }
                            }
                            @Override
                            public void processBroadcastElement(Tuple2<String, Employee> stringEmployeeTuple2, KeyedBroadcastProcessFunction<String, Tuple2<String, EmployeeLocation>, Tuple2<String, Employee>, Tuple2<String, EnrichedEmployee>>.Context context, Collector<Tuple2<String, EnrichedEmployee>> collector) throws Exception {
                                // Updating the broadcast state with incoming Employee data.
                                // This allows the state to be accessed by all instances of the operator for joining.
                                context.getBroadcastState(employeeStateDescriptor).put(stringEmployeeTuple2.f0, stringEmployeeTuple2.f1);
                            }
                        });
        // Define Kafka serialization schema for transformed data
        KafkaRecordSerializationSchema<Tuple2<String, EnrichedEmployee>> serializationSchema = new KafkaRecordSerializationSchema<>() {
            @Override
            public ProducerRecord<byte[], byte[]> serialize(Tuple2<String, EnrichedEmployee> enrichedEmployeeTuple2, KafkaSinkContext kafkaSinkContext, Long aLong) {
                ObjectMapper objectMapper = new ObjectMapper();
                String key = enrichedEmployeeTuple2.f0;
                String value = null;
                try {
                    value = objectMapper.writeValueAsString(enrichedEmployeeTuple2.f1);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                return new ProducerRecord<>(OUTPUT_TOPIC, key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
            }
        };

        // Define and add the sink to the Kafka topic for transformed data
        KafkaSink<Tuple2<String, EnrichedEmployee>> sinkKafka = KafkaSink.<Tuple2<String, EnrichedEmployee>>builder()
                .setBootstrapServers(config.getString(BROKERS))
                .setRecordSerializer(serializationSchema)
                .build();

        enriched.sinkTo(sinkKafka);

        // Execute the Flink streaming job
        env.execute("EnrichEmployeeLocationJob");
    }
}