#!/bin/sh

ansible-playbook -i ansible/inventory/hosts.yaml ansible/spark-cluster/install-spark.yaml 