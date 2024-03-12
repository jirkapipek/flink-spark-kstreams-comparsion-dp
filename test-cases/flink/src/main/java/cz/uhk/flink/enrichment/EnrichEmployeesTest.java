package cz.uhk.flink.enrichment;

import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.model.json.Employee;
import cz.uhk.model.json.EmployeeLocation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;


import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class EnrichEmployeesTest {

  static final String INPUT_TOPIC = "EMPLOYEES"; // Input Kafka topic for employee data
  static final String INPUT_LOCATION_TOPIC = "EMPLOYEE_LOCATION"; // Input Kafka topic for employee location data
  static final String OUTPUT_TOPIC = "EMPLOYEES_ENRICHED"; // Output Kafka topic for enriched employee data
  private static final String BROKERS = "kafka.brokers"; // Kafka brokers configuration key
  private static final String GROUP_ID = "kafka.groupId"; // Kafka group ID configuration key
  private static final String CHECKPOINT_LOCATION = "flink.checkpoint"; // Spark checkpoint location configuration key

  public static void main(final String[] args) throws ConfigurationException, IOException {
    // Main method to handle the stream processing application execution
  }

  // Method to build and execute the stream processing logic
  static void buildStream() throws TimeoutException, ConfigurationException {
    BaseHierarchicalConfiguration config = ConfigurationManager.getInstance().getConfig(); // Load configuration
  }
}