package cz.uhk.kstreams.aggregation;

import cz.uhk.kstreams.serializers.JsonGenericDeserializer;
import cz.uhk.kstreams.serializers.JsonGenericSerializer;
import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.kstreams.KStreamManager;
import cz.uhk.model.json.EmployeeLocation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.IOException;
import java.util.Properties;

@Log4j2
public class CountEmployeesInLabTest {

  // Define input and output topics
  static final String INPUT_TOPIC = "EMPLOYEE_LOCATION";
  static final String OUTPUT_TOPIC = "EMPLOYEE_LOCATION_AGGREGATED";

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
    // Create Serde for EmployeeLocation objects
    Serde<EmployeeLocation> employeeLocationSerde = Serdes.serdeFrom(new JsonGenericSerializer<>(), new JsonGenericDeserializer<>(EmployeeLocation.class));

    // Load Kafka Streams configuration properties
    Properties config = KStreamManager.getInstance().loadProperties(kStreamsConfig);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG,true);

    // Initialize StreamsBuilder
    StreamsBuilder builder = new StreamsBuilder();

    // Create a stream from the input topic
    KStream<String, EmployeeLocation> employeeLocation = builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), employeeLocationSerde));

    // Perform aggregation to count employees in each lab
    KTable<String, Long> employeeLabCount = employeeLocation
            .map((key, value) -> KeyValue.pair(value.lab, 1L))
            .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
            .count();

    // Convert the aggregated counts to strings and write to the output topic
    KTable<String, String> stringifiedStream = employeeLabCount.mapValues(value -> value.toString());
    stringifiedStream.toStream().to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

    // Build and return KafkaStreams object
    return new KafkaStreams(builder.build(), config);
  }

}
