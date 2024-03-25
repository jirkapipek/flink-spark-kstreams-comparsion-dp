#!/bin/sh

ansible-playbook -i ansible/inventory/hosts.yaml ansible/flink-cluster/install-flink.yaml 
