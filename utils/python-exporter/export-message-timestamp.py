from confluent_kafka import Consumer, KafkaException
import pandas as pd

# Kafka consumer setup
conf = {
    'bootstrap.servers': 'virualserver1:9092',  # Change according to your configuration
    'group.id': 'my_consumer_group',  # Specify your consumer group
    'auto.offset.reset': 'earliest'
}
consumer = Consumer(conf)
topics = ['EMPLOYEES', 'EMPLOYEES_TRANSFORMED']

# Subscribe to topics
consumer.subscribe(topics)

# Reading messages and retrieving timestamps
timestamps_input = []
timestamps_output = []

try:
    while True:
        msg = consumer.poll(timeout=1.0)
        if msg is None:
            break
        if msg.error():
            if msg.error().code() == KafkaException._PARTITION_EOF:
                # End of partition
                continue
            else:
                print(msg.error())
                break
        # Save timestamps based on topic
        if msg.topic() == 'EMPLOYEES':
            timestamps_input.append(msg.timestamp()[1])
        elif msg.topic() == 'EMPLOYEES_TRANSFORMED':
            timestamps_output.append(msg.timestamp()[1])
except KeyboardInterrupt:
    pass
finally:
    consumer.close()

# Convert timestamps to pandas DataFrame
df_input = pd.DataFrame(timestamps_input, columns=['Input Topic Timestamp'])
df_output = pd.DataFrame(timestamps_output, columns=['Output Topic Timestamp'])

# Merge dataframes into one
df = pd.concat([df_input, df_output], axis=1)

# Save to Excel
df.to_excel('kafka_timestamps.xlsx', index=False)

print("Timestamps have been saved to Excel.")
