package cz.uhk.kstreams.enrichment;

import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.kstreams.KStreamManager;
import cz.uhk.kstreams.serializers.JsonGenericDeserializer;
import cz.uhk.kstreams.serializers.JsonGenericSerializer;
import cz.uhk.model.json.Employee;
import cz.uhk.model.json.EmployeeLocation;
import cz.uhk.model.json.EnrichedEmployee;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.IOException;
import java.util.Properties;

@Log4j2
public class EnrichEmployeesTest {

  static final String INPUT_TOPIC = "EMPLOYEES";

  static final String INPUT_LOCATION_TOPIC = "EMPLOYEE_LOCATION";

  static final String OUTPUT_TOPIC = "EMPLOYEES_ENRICHED";
  public static void main(final String[] args) throws ConfigurationException, IOException {
    // Setup logging configuration
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

    // Shutdown hook to close Kafka Streams gracefully
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        streams.close();
      }
    }));
  }

  // Method to build Kafka Streams application
  static KafkaStreams buildStream(String kStreamsConfig) throws ConfigurationException, IOException {
    // Initialize serializers and deserializers
    Serde<EmployeeLocation> employeeLocationSerde = Serdes.serdeFrom(new JsonGenericSerializer<>(), new JsonGenericDeserializer<>(EmployeeLocation.class));
    Serde<Employee> employeeSerde = Serdes.serdeFrom(new JsonGenericSerializer<>(), new JsonGenericDeserializer<>(Employee.class));
    Serde<EnrichedEmployee> enrichedEmployeeSerde = Serdes.serdeFrom(new JsonGenericSerializer<>(), new JsonGenericDeserializer<>(EnrichedEmployee.class));

    // Load configuration properties
    Properties config = KStreamManager.getInstance().loadProperties(kStreamsConfig);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG,true);
    config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,Serdes.String().getClass().getName());
    config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,Serdes.String().getClass().getName());

    // Create a StreamsBuilder instance
    StreamsBuilder builder = new StreamsBuilder();

    // Create KStreams for employee and employee location data
    KStream<String, Employee> employee = builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), employeeSerde));
    KStream<String, EmployeeLocation> employeeLocation = builder.stream(INPUT_LOCATION_TOPIC, Consumed.with(Serdes.String(), employeeLocationSerde));

    // Group employee KStream by key and reduce to obtain the latest employee state
    KTable<String, Employee> employeeTable = employee
            .groupByKey(Grouped.with(Serdes.String(), employeeSerde))
            .reduce((oldValue, newValue) -> newValue);

    // Perform a left join between employee location KStream and employee KTable to enrich employee data
    KStream<String, EnrichedEmployee> enrichedEmployeeStream = employeeLocation
            .selectKey((employeeId, employeeLocationData) -> employeeId) // Convert key to string
            .leftJoin(employeeTable, (employeeLocationData, employeeData) -> {
              if (employeeData != null) {
                EnrichedEmployee enriched = new EnrichedEmployee();
                enriched.employeeId = employeeData.employeeId;
                enriched.firstName = employeeData.firstName;
                enriched.lastName = employeeData.lastName;
                enriched.lab = employeeLocationData.lab;
                return enriched;
              } else {
                return null; // Return null if employee is not found in the table
              }
            });

    // Write enriched employee data to output topic
    enrichedEmployeeStream.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), enrichedEmployeeSerde));

    // Build and return KafkaStreams instance
    return new KafkaStreams(builder.build(), config);
  }
}