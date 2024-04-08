# Proudy událostí a nástroje na jejich zpracování

Tento projekt je součástí diplomové práce na téma "Proudy událostí a nástroje na jejich zpracování". Cílem projektu je prozkoumat a porovnat různé nástroje pro zpracování proudů událostí, jako jsou Apache Flink, Apache Spark a Kafka Streams.

## Prerekvizity pro spuštění testovacího prostředí

Pro spuštění testovacího prostředí tohoto projektu je potřeba mít nainstalováno následující:

- **Operační systém:** Linux (Ubuntu/Debian)
- **Java:** JDK 11 (Java Development Kit) musí být nainstalován.
- **Maven:** Nástroj pro automatizaci sestavení softwaru (build tool). Maven se používá pro správu projektů, závislostí a pro automatizaci build procesu. Je nezbytný pro sestavení Java aplikací, které jsou součástí tohoto projektu.
- **Vagrant:** Nástroj pro vytváření a konfiguraci virtuálních prostředí s jednotnou konfigurací napříč různými platformami.
- **VirtualBox:** Software pro virtualizaci, který Vagrant používá k vytváření virtuálních strojů.
- **Docker:** Platforma pro vývoj, doručení a spuštění aplikací v kontejnerech. Umožňuje snadné balení a distribuci aplikací.
- **Git:** Git je nezbytný pro klonování repozitářů a správu verzí vašich aplikací.
## Instalace prerekvizit

### Linux (Ubuntu/Debian)

1. **Aktualizace systému:**
   ```sudo apt update && sudo apt upgrade```
2. **Instalace Javy:**
   ```sudo apt-get install openjdk-11-jdk```
3. **Instalace Maven:**
   ```sudo apt install maven```   
4. **Instalace Vagrantu**
   ```sudo apt install vagrant```
5. **Instalace VirtualBoxu**
   ```sudo apt install virtualbox```
6. **Instalace Dockeru**
   ```sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin```
7. **Instalace Gitu**
   ```sudo apt install git```   

### Další kroky

Po úspěšné instalaci všech prerekvizit je možné pokračovat k nastavení a spuštění testovacího prostředí podle návodu k tomuto projektu.

## Naklonování GIT

```
sudo git clone https://github.com/jirkapipek/flink-spark-kstreams-comparsion-dp.git
cd flink-spark-kstreams-comparsion-dp
```

## Spuštění testovacího prostředí pomocí `start-stack.sh`

```
cd iac
./start-stack.sh
cd ..
```

Skript `start-stack.sh` je klíčovým nástrojem pro automatizaci nastavení a spuštění celého testovacího prostředí. Spuštěním tohoto skriptu se postupně provedou následující kroky:

1. **Přechod do adresáře s Vagrantem a spuštění virtuálních strojů pomocí `vagrant up`**
Tento krok inicializuje a spustí všechny virtuální stroje definované ve Vagrant konfiguračních souborech.

    ```
    cd vagrant
    vagrant up
    cd ..
    ```

2. **Nasazení Kafka Clusteru s použitím Ansible** 
    ```
    ansible-playbook -i ansible/inventory/hosts.yaml ansible/kafka-cluster/install-kafka.yml
    ```

3. **Nasazení monitoring nástrojů**

    ```
    ansible-playbook -i ansible/inventory/hosts.yaml ansible/monitoring-tools/playbooks/monitoring.yml
    ```

4. **Instalace Spark a Flink clusteru**

    ```
    ansible-playbook -i ansible/inventory/hosts.yaml ansible/spark-cluster/install-spark.yaml 
    ansible-playbook -i ansible/inventory/hosts.yaml ansible/flink-cluster/install-flink.yaml 
    ```

5. **Nastavení Kafka konektorů pro generování testovacích dat** 

    - Konfigurace EMPLOYEE-CONNECTOR:
      ```
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
      ```

    - Konfigurace EMPLOYEE_LOCATION-CONNECTOR:
      ```
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
      ```

Tento proces nasadí kompletní a automatizované prostředí pro testování vyvinutých aplikací.

## Manuální nasazení aplikací

I přes výhody automatizace bylo využito manuálního nasazení, především z důvodů lepší kontroly a flexibilitě při koordinaci nasazování různých verzí aplikací. 

Manuální proces nasazení zahrnuje následující kroky:

### Build aplikací s použitím Maven

Pro build aplikací Apache Spark, Apache Flink a Kafka Streams, které jsou umístěny v adresářích `test-cases/spark`, `test-cases/flink` a `test-cases/kstreams`, je třeba použít následující příkazy v terminálu. Každý krok kompiluje a vytváří spustitelný balíček pro danou aplikaci.

1. **Build Apache Spark aplikace**
```
cd test-cases/spark
mvn clean package
```

2. **Build Apache Flink aplikace**
```
cd test-cases/flink
mvn clean package
```

3. **Build Kafka Streams aplikace**
```
cd test-cases/kstreams
mvn clean package
```

### Přenos `.jar` souborů na server

S použitím `scp` příkazu lze `.jar` soubory přenést na příslušné servery.

```
sudo scp test-cases/flink/target/flink-tests-1.0-SNAPSHOT.jar vagrant@virtualserver2:/opt/flink/
sudo scp test-cases/spark/target/spark-tests-1.0-SNAPSHOT.jar vagrant@virtualserver2:/opt/spark/
sudo scp test-cases/kstreams/target/kstreams-tests-1.0-SNAPSHOT.jar vagrant@virtualserver2:/opt/kstreams/
```

### Spuštění aplikací

S použitím `scp` příkazu byly `.jar` soubory přeneseny na příslušné servery.

1. **Spuštění Apache Spark aplikace**
```
ssh vagrant@virtualserver2
cd /opt/spark-3.2.0-bin-hadoop3.2
# PŘÍKLAD SPUŠTĚNÍ TESTU FILTRACE
./bin/spark-submit --class cz.uhk.spark.filter.FilterEmployeesTest --master spark://virtualserver2:7077 /opt/spark/spark-tests-1.0-SNAPSHOT.jar
```
2. **Spuštění Apache Flink aplikace**
```
ssh vagrant@virtualserver2
cd /opt/flink-1.18.1-bin-scala_2.12
# PŘÍKLAD SPUŠTĚNÍ TESTU FILTRACE
./bin/flink run --class cz.uhk.flink.filter.FilterEmployeesTest --detached /opt/flink/flink-tests-1.0-SNAPSHOT.jar
```
3. **Spuštění Kafka Streams aplikace**
```
ssh vagrant@virtualserver2
# PŘÍKLAD SPUŠTĚNÍ TESTU FILTRACE
java -cp kstreams-app.jar cz.uhk.kstreams.filter.FilterEmployeesTest /opt/kstreams/kstreams-tests-1.0-SNAPSHOT.jar
```

