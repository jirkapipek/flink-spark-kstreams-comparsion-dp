package cz.uhk.spark.enrichment;

import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.model.json.Employee;
import cz.uhk.model.json.EmployeeLocation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class EnrichEmployeesTest {

  static final String INPUT_TOPIC = "EMPLOYEES"; // Input Kafka topic for employee data
  static final String INPUT_LOCATION_TOPIC = "EMPLOYEE_LOCATION"; // Input Kafka topic for employee location data
  static final String OUTPUT_TOPIC = "EMPLOYEES_ENRICHED"; // Output Kafka topic for enriched employee data
  private static final String BROKERS = "kafka.brokers"; // Kafka brokers configuration key
  private static final String GROUP_ID = "kafka.groupId"; // Kafka group ID configuration key
  private static final String CHECKPOINT_LOCATION = "spark.checkpoint"; // Spark checkpoint location configuration key

  public static void main(final String[] args) throws ConfigurationException, IOException {
    // Main method to handle the stream processing application execution
    try {
      buildStream(); // Build and start the stream processing
    } catch (TimeoutException | StreamingQueryException e) {
      throw new RuntimeException(e);
    }
  }

  // Method to build and execute the stream processing logic
  static void buildStream() throws TimeoutException, StreamingQueryException, ConfigurationException {
    BaseHierarchicalConfiguration config = ConfigurationManager.getInstance().getConfig(); // Load configuration
    SparkSession spark = SparkSession // Create a Spark session
            .builder()
            .appName(config.getString(GROUP_ID)) // Set application name from config
            .config("spark.master", "local") // Set master to local
            .getOrCreate();
    spark.sparkContext().setLogLevel("WARN"); // Set log level to WARN

    // Define schema for employee data
    StructType schema = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("employee_id", DataTypes.IntegerType, false),
            DataTypes.createStructField("first_name", DataTypes.StringType, true),
            DataTypes.createStructField("last_name", DataTypes.StringType, true),
            DataTypes.createStructField("age", DataTypes.IntegerType, true),
            DataTypes.createStructField("ssn", DataTypes.StringType, true),
            DataTypes.createStructField("hourly_rate", DataTypes.DoubleType, true),
            DataTypes.createStructField("gender", DataTypes.StringType, true),
            DataTypes.createStructField("email", DataTypes.StringType, true)
    });

    // Define schema for employee location data
    StructType schemaLocation = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("employee_id", DataTypes.IntegerType, false),
            DataTypes.createStructField("lab", DataTypes.StringType, true),
            DataTypes.createStructField("department_id", DataTypes.IntegerType, true),
            DataTypes.createStructField("arrival_date", DataTypes.IntegerType, true)
    });

    // Read employee location data from Kafka and deserialize
    Dataset<EmployeeLocation> employeeLocation = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", config.getString(BROKERS))
            .option("subscribe", INPUT_LOCATION_TOPIC)
            .option("startingOffsets", "earliest")
            .option("minPartitions", "8")
            .load()
            .selectExpr("CAST(key AS STRING) as key", "CAST(value AS STRING) as message")
            .select(functions.col("key"), functions.from_json(functions.col("message"), schemaLocation).as("json"))
            .select("key", "json.*")
            .as(Encoders.bean(EmployeeLocation.class));

    // Read employee data from Kafka, deserialize, and remove duplicates based on the key
    Dataset<Employee> employees = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", config.getString(BROKERS))
            .option("subscribe", INPUT_TOPIC)
            .option("startingOffsets", "earliest")
            .load()
            .selectExpr("CAST(key AS STRING) as key", "CAST(value AS STRING) as message")
            .select(functions.col("key"), functions.from_json(functions.col("message"), schema).as("json"))
            .select("key", "json.*")
            .as(Encoders.bean(Employee.class))
            .dropDuplicates("key");

    // Join employee and location data on employee_id and start the stream processing
    Dataset<Row> joined = employees
            .join(employeeLocation, employees.col("key").equalTo(employeeLocation.col("key")));

    StreamingQuery query = joined
            .selectExpr("to_json(struct(*)) AS value")
            .writeStream()
            .format("kafka")
            .option("checkpointLocation", config.getString(CHECKPOINT_LOCATION))
            .option("kafka.bootstrap.servers", config.getString(BROKERS))
            .option("topic", OUTPUT_TOPIC)
            .start();

    query.awaitTermination(); // Wait for the query to finish
  }
}