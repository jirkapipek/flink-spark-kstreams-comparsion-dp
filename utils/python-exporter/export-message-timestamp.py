from confluent_kafka import Consumer, KafkaException
import pandas as pd

# Nastavení Kafka consumeru
conf = {
    'bootstrap.servers': 'localhost:9095',  # Změňte dle vaší konfigurace
    'group.id': 'my_consumer_group',  # Specifikujte consumer group
    'auto.offset.reset': 'earliest'
}
consumer = Consumer(conf)
topics = ['EMPLOYEE_LOCATION_AGGREGATED', 'EMPLOYEES_TRANSFORMED']

# Připojení k topics
consumer.subscribe(topics)

# Čtení zpráv a získání timestampů
timestamps_input = []
timestamps_output = []

try:
    while True:
        msg = consumer.poll(timeout=1.0)
        if msg is None:
            break
        if msg.error():
            if msg.error().code() == KafkaException._PARTITION_EOF:
                # Konec partition
                continue
            else:
                print(msg.error())
                break
        # Uložení timestampů podle topicu
        if msg.topic() == 'EMPLOYEE_LOCATION_AGGREGATED':
            
            timestamps_input.append(msg.timestamp()[1])
        elif msg.topic() == 'EMPLOYEES_TRANSFORMED':
            timestamps_output.append(msg.timestamp()[1])
except KeyboardInterrupt:
    pass
finally:
    consumer.close()

# Převod timestampů na pandas DataFrame
df_input = pd.DataFrame(timestamps_input, columns=['Input Topic Timestamp'])
df_output = pd.DataFrame(timestamps_output, columns=['Output Topic Timestamp'])

# Sloučení dataframes do jednoho
df = pd.concat([df_input, df_output], axis=1)

# Uložení do Excelu
df.to_excel('kafka_timestamps.xlsx', index=False)

print("Timestampy byly uloženy do Excelu.")