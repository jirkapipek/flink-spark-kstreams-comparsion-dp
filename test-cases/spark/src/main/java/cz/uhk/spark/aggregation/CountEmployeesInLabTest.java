package cz.uhk.spark.aggregation;

import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.model.json.EmployeeLocation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class CountEmployeesInLabTest {

  // Define input and output topics
  private static final String INPUT_TOPIC = "EMPLOYEE_LOCATION";
  private static final String OUTPUT_TOPIC = "EMPLOYEE_LOCATION_AGGREGATED";
  private static final String BROKERS = "kafka.brokers";
  private static final String GROUP_ID = "kafka.groupId";
  private static final String CHECKPOINT_LOCATION = "spark.checkpoint";

  public static void main(final String[] args) throws ConfigurationException {

    try {
      buildStream();
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    } catch (StreamingQueryException e) {
      throw new RuntimeException(e);
    }
  }

  // Method to build and execute the Spark Structured Streaming application
  static void buildStream() throws TimeoutException, StreamingQueryException, ConfigurationException {
    // Load configuration settings
    BaseHierarchicalConfiguration config = ConfigurationManager.getInstance().getConfig();

    // Initialize Spark session
    SparkSession spark = SparkSession
            .builder()
            .appName(config.getString(GROUP_ID))
            .config("spark.master", "local")
            .getOrCreate();
    // Set log level to warn
    spark.sparkContext().setLogLevel("WARN");

    // Define schema for incoming data
    StructType schema = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("employee_id", DataTypes.IntegerType, false),
            DataTypes.createStructField("lab", DataTypes.StringType, true),
            DataTypes.createStructField("department_id", DataTypes.IntegerType, true),
            DataTypes.createStructField("arrival_date", DataTypes.IntegerType, true)
    });

    // Create dataset for employee location data from Kafka source
    Dataset<EmployeeLocation> employeeLocationDataset = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", config.getString(BROKERS))
            .option("subscribe", INPUT_TOPIC)
            .option("startingOffsets", "earliest")
            .load()
            .selectExpr("CAST(value AS STRING) as message")
            .select(functions.from_json(functions.col("message"), schema).as("json"))
            .select("json.*")
            .as(Encoders.bean(EmployeeLocation.class));

    // Aggregate data by lab and count employees, then write back to Kafka
    StreamingQuery query = employeeLocationDataset
            .select("*")
            .groupBy("lab")
            .count()
            .selectExpr("lab as key", "CAST(count AS STRING) as value")
            .writeStream()
            .format("kafka")
            .option("checkpointLocation", config.getString(CHECKPOINT_LOCATION))
            .option("kafka.bootstrap.servers", config.getString(BROKERS))
            .option("topic", OUTPUT_TOPIC)
            .outputMode("update")
            .trigger(Trigger.ProcessingTime("1 seconds"))
            .start();

    // Await termination of the query
    query.awaitTermination();
  }
}