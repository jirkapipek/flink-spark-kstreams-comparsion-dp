package cz.uhk.flink.transformation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.configuration.ConfigurationManager;
import cz.uhk.model.json.Employee;
import cz.uhk.model.json.EmployeeWithSalary;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Log4j2
public class TransformationEmployeesSalaryTest {

  // Constants for Kafka topic names and configuration keys
  static final String INPUT_TOPIC = "EMPLOYEES";
  static final String OUTPUT_TOPIC = "EMPLOYEES_TRANSFORMED";
  private static final String BROKERS = "kafka.brokers";
  private static final String GROUP_ID = "kafka.groupId";
  private static final String CHECKPOINT_LOCATION = "flink.checkpoint";

  public static void main(final String[] args) throws ConfigurationException, IOException {
    try {
      buildStream();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // Builds and executes the Kafka Streams pipeline
  static void buildStream() throws Exception {
    int WORK_HOURS_PER_MONTH = 160; // Constant for calculating monthly salary
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
                return TypeInformation.of(new TypeHint<>() {});
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

    // Create a data stream from the source
    DataStream<Tuple2<String, Employee>> employeeDataStream = env
            .fromSource(employeeKafkaSource, WatermarkStrategy.noWatermarks(), "employees_source");

    // Transform Employee data to include monthly salary
    DataStream<Tuple2<String, EmployeeWithSalary>> transformedDataStream = employeeDataStream.map(employeeTuple -> {
      EmployeeWithSalary transformedEmployee = new EmployeeWithSalary();
      double monthlySalary = employeeTuple.f1.hourly_rate * WORK_HOURS_PER_MONTH;
      transformedEmployee.employeeId = employeeTuple.f1.employee_id;
      transformedEmployee.firstName = employeeTuple.f1.first_name;
      transformedEmployee.lastName = employeeTuple.f1.last_name;
      transformedEmployee.age = employeeTuple.f1.age;
      transformedEmployee.ssn = employeeTuple.f1.ssn;
      transformedEmployee.email = employeeTuple.f1.email;
      transformedEmployee.gender = employeeTuple.f1.gender;
      transformedEmployee.salary = monthlySalary;

      return new Tuple2<>(employeeTuple.f0, transformedEmployee);
    }).returns(new TypeHint<>() {});

    // Define Kafka serialization schema for transformed data
    KafkaRecordSerializationSchema<Tuple2<String, EmployeeWithSalary>> serializationSchema = new KafkaRecordSerializationSchema<>() {
      @Override
      public ProducerRecord<byte[], byte[]> serialize(Tuple2<String, EmployeeWithSalary> stringEmployeeTuple2, KafkaSinkContext kafkaSinkContext, Long aLong) {
        ObjectMapper objectMapper = new ObjectMapper();
        String key = stringEmployeeTuple2.f0;
        String value = null;
        try {
          value = objectMapper.writeValueAsString(stringEmployeeTuple2.f1);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }

        return new ProducerRecord<>(OUTPUT_TOPIC, key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
      }
    };

    // Define and add the sink to the Kafka topic for transformed data
    KafkaSink<Tuple2<String, EmployeeWithSalary>> sinkKafka = KafkaSink.<Tuple2<String, EmployeeWithSalary>>builder()
            .setBootstrapServers(config.getString(BROKERS))
            .setRecordSerializer(serializationSchema)
            .build();

    transformedDataStream.sinkTo(sinkKafka);

    // Execute the Flink streaming job
    env.execute("TransformationEmployeesSalary");
  }
}