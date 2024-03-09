package cz.uhk.kstreams.transformation;

import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.kstreams.KStreamManager;
import cz.uhk.kstreams.serializers.JsonGenericDeserializer;
import cz.uhk.kstreams.serializers.JsonGenericSerializer;
import cz.uhk.model.json.Employee;
import cz.uhk.model.json.EmployeeWithSalary;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.IOException;
import java.util.Properties;

@Log4j2
public class TransformationEmployeesSalaryTest {

  // Input and output topics
  static final String INPUT_TOPIC = "EMPLOYEES";
  static final String OUTPUT_TOPIC = "EMPLOYEES_TRANSFORMED";

  public static void main(final String[] args) throws ConfigurationException, IOException {
    // Load Log4j configuration
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

    // Clean up and start the Kafka Streams application
    streams.cleanUp();
    streams.start();

    // Gracefully shutdown the Kafka Streams application on JVM exit
    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
  }

  static KafkaStreams buildStream(String kStreamsConfig) throws ConfigurationException, IOException {
    // Initialize serializers and configuration
    Serde<Employee> employeeSerde = Serdes.serdeFrom(new JsonGenericSerializer<>(), new JsonGenericDeserializer<>(Employee.class));
    Serde<EmployeeWithSalary> employeeWithSalarySerde = Serdes.serdeFrom(new JsonGenericSerializer<>(), new JsonGenericDeserializer<>(EmployeeWithSalary.class));
    Properties config = KStreamManager.getInstance().loadProperties(kStreamsConfig);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG,true);

    // Create StreamsBuilder instance
    StreamsBuilder builder = new StreamsBuilder();

    // Define work hours per month
    int WORK_HOURS_PER_MONTH = 160;

    // Create stream from input topic
    KStream<String, Employee> employee = builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), employeeSerde));

    // Map values to compute monthly salary and create EmployeeWithSalary objects
    KStream<String, EmployeeWithSalary> employeeWithMonthlySalary = employee.mapValues(em -> {
      double monthlySalary = em.hourlyRate * WORK_HOURS_PER_MONTH;
      EmployeeWithSalary employeeWithSalary = new EmployeeWithSalary();
      employeeWithSalary.employeeId = em.employeeId;
      employeeWithSalary.email = em.email;
      employeeWithSalary.age = em.age;
      employeeWithSalary.ssn = em.ssn;
      employeeWithSalary.firstName = em.firstName;
      employeeWithSalary.lastName = em.lastName;
      employeeWithSalary.gender = em.gender;
      employeeWithSalary.salary = monthlySalary;
      return employeeWithSalary;
    });

    // Write transformed data to output topic
    employeeWithMonthlySalary.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), employeeWithSalarySerde));

    // Return KafkaStreams instance with built topology and configuration
    return new KafkaStreams(builder.build(), config);
  }
}