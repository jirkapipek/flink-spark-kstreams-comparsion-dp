from confluent_kafka import Consumer, KafkaException
import pandas as pd
import json

# Kafka consumer setup
conf = {
    'bootstrap.servers': 'localhost:9095',
    'group.id': 'my_consumer_group',
    'auto.offset.reset': 'earliest'
}

consumer = Consumer(conf)
topics = ['EMPLOYEES', 'EMPLOYEES_FILTERED']

consumer.subscribe(topics)

# Prepare to store filtered data
timestamps_input = []
timestamps_output = []

try:
    while True:
        msg = consumer.poll(timeout=1.0)
        if msg is None:
            break
        if msg.error():
            if msg.error().code() == KafkaException._PARTITION_EOF:
                continue
            else:
                print(msg.error())
                break
        
        # Assuming your message payload is a JSON string that looks like the "arg.properties" structure
        if msg.value():
            employee_details = json.loads(msg.value().decode('utf-8'))
            
            # Apply filter criteria: gender == 'female' and hourly_rate > 15
            if employee_details['gender'] == 'female' and employee_details['hourly_rate'] > 15:
                # Adjust these conditions based on actual topic and data structure
                if msg.topic() == 'EMPLOYEES':
                    timestamps_input.append(msg.timestamp()[1])
                elif msg.topic() == 'EMPLOYEES_FILTERED':
                    timestamps_output.append(msg.timestamp()[1])
except KeyboardInterrupt:
    pass
finally:
    consumer.close()

# Convert timestamps to pandas DataFrame
df_input = pd.DataFrame(timestamps_input, columns=['Input Topic Timestamp'])
df_output = pd.DataFrame(timestamps_output, columns=['Output Topic Timestamp'])

# Merge dataframes
df = pd.concat([df_input, df_output], axis=1)

# Save to Excel
df.to_excel('kafka_filtered_timestamps.xlsx', index=False)

print("Filtered timestamps have been saved to Excel.")