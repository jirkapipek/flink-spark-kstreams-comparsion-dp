#!/bin/sh

cd vagrant

vagrant up

cd ..

ansible-playbook -i ansible/inventory/hosts.yaml ansible/kafka-cluster/install-kafka.yml

ansible-playbook -i ansible/inventory/hosts.yaml ansible/monitoring-tools/playbooks/monitoring.yml

#ansible-playbook -i ansible/inventory/hosts.yaml ansible/spark-cluster/install-spark.yaml 
# ansible-playbook -i ansible/inventory/hosts.yaml ansible/flink-cluster/install-flink.yaml 
#ansible-playbook -i ansible/inventory/hosts.yaml ansible/flink-cluster/run-flink-job.yaml
curl --location --request PUT 'http://virtualserver1:8083/connectors/EMPLOYEE-CONNECTOR/config' --header 'Content-Type: application/json' --data '{
        "connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
        "key.converter": "org.apache.kafka.connect.storage.StringConverter",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "value.converter.schemas.enable": "false",
        "kafka.topic": "EMPLOYEES",
        "quickstart": "payroll_employee",
        "max.interval": 5,
        "iterations": 10000000,
        "tasks.max": "1"
      }'

curl --location --request PUT 'http://virtualserver1:8083/connectors/EMPLOYEE_LOCATION-CONNECTOR/config' --header 'Content-Type: application/json' --data '{
        "connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
        "key.converter": "org.apache.kafka.connect.storage.StringConverter",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "value.converter.schemas.enable": "false",
        "kafka.topic": "EMPLOYEE_LOCATION",
        "quickstart": "payroll_employee_location",
        "max.interval": 1,
        "iterations": 10000000,
        "tasks.max": "1"
      }'