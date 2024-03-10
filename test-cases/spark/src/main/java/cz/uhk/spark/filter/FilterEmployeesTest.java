package cz.uhk.spark.filter;

import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.model.json.Employee;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Log4j2
public class FilterEmployeesTest {

    // Define input and output topics
    static final String INPUT_TOPIC = "EMPLOYEES";
    static final String OUTPUT_TOPIC = "EMPLOYEES_FILTERED";

    private static final String BROKERS = "kafka.brokers";
    private static final String GROUP_ID = "kafka.groupId";

    public static void main(final String[] args) throws ConfigurationException, IOException {

        try {
            buildStream();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (StreamingQueryException e) {
            throw new RuntimeException(e);
        }


    }

    // Method to build Kafka Streams application
    static void buildStream() throws TimeoutException, StreamingQueryException, ConfigurationException {
        BaseHierarchicalConfiguration config = ConfigurationManager.getInstance().getConfig();

        SparkSession spark = SparkSession
                .builder()
                .appName(config.getString(GROUP_ID))
                .config("spark.master", "local")
                .getOrCreate();
        spark.sparkContext().setLogLevel("WARN");

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

        Dataset<Employee> df = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", config.getString(BROKERS))
                .option("subscribe", INPUT_TOPIC)
                .option("startingOffsets", "earliest")
                .option("minPartitions", "8")
                .load()
                .selectExpr("CAST(value AS STRING) as message")
                .select(functions.from_json(functions.col("message"), schema).as("json"))
                .select("json.*")
                .as(Encoders.bean(Employee.class));

        StreamingQuery query = df.select("*")
                .where("gender == 'female' AND hourly_rate > 15")
                .selectExpr("CAST(employee_id AS STRING) as key", "to_json(struct(*)) AS value")
                .writeStream()
                .format("kafka")
                .option("checkpointLocation", "src/main/checkpoint/request")
                .option("kafka.bootstrap.servers", config.getString(BROKERS))
                .option("topic", OUTPUT_TOPIC)
                .start();

        query.awaitTermination();
    }
}