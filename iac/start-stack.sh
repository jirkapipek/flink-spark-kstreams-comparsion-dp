#!/bin/sh

cd vagrant

vagrant up

cd ..

ansible-playbook -i ansible/inventory/hosts.yaml ansible/kafka-cluster/install-kafka.yml

ansible-playbook -i ansible/inventory/hosts.yaml ansible/monitoring-tools/playbooks/monitoring.yml