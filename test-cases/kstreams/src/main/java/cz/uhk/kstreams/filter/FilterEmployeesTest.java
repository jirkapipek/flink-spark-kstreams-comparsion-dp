package cz.uhk.kstreams.filter;

import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.kstreams.KStreamManager;
import cz.uhk.kstreams.serializers.JsonGenericDeserializer;
import cz.uhk.kstreams.serializers.JsonGenericSerializer;
import cz.uhk.model.json.Employee;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.IOException;
import java.util.Properties;

@Log4j2
public class FilterEmployeesTest {

    // Define input and output topics
    static final String INPUT_TOPIC = "EMPLOYEES";
    static final String OUTPUT_TOPIC = "EMPLOYEES_FILTERED";

    public static void main(final String[] args) throws ConfigurationException, IOException {
        // Load log4j configuration
        String log4jConfig = ConfigurationManager.getResourcePath("log4j2.properties");
        if (log4jConfig != null) {
            log.info("Reading 'log4j2.properties' from filesystem: [{}]", log4jConfig);
            System.setProperty("log4j.configurationFile", log4jConfig);
            LoggerContext.getContext(false).reconfigure();
        } else {
            log.info("Reading 'log4j2.properties' from JAR");
        }

        // Build Kafka Streams application
        final KafkaStreams streams = buildStream("kStreams.properties");

        // Start the Kafka Streams application
        streams.cleanUp();
        streams.start();

        // Add shutdown hook to handle graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                streams.close();
            }
        }));
    }

    // Method to build Kafka Streams application
    static KafkaStreams buildStream(String kStreamsConfig) throws ConfigurationException, IOException {
        // Create Serde for Employee objects
        Serde<Employee> employeeSerde = Serdes.serdeFrom(new JsonGenericSerializer<>(), new JsonGenericDeserializer<>(Employee.class));

        // Load Kafka Streams configuration properties
        Properties config = KStreamManager.getInstance().loadProperties(kStreamsConfig);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, true);

        // Initialize StreamsBuilder
        StreamsBuilder builder = new StreamsBuilder();

        // Create a stream from the input topic
        KStream<String, Employee> employee = builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), employeeSerde));

        // Apply filter to select female employees with hourly rate greater than 15
        KStream<String, Employee> filteredEmployee = employee.filter((key, value) -> "female".equals(value.gender) && value.hourlyRate > 15);

        // Write filtered employees to the output topic
        filteredEmployee.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), employeeSerde));

        // Build and return KafkaStreams object
        return new KafkaStreams(builder.build(), config);
    }

}