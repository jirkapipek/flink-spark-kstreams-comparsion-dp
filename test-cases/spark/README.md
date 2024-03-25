# Spuštění aplikace

1. ```mvn clean package```
2. Pro spuštění joinu: ```mvn exec:java -Dexec.mainClass="JoinFiltered"```
3. Pro spuštění filtrování požadavků: ```mvn exec:java -Dexec.mainClass="RequestFilter"```
4. Pro spuštění filtrování odpovědí: ```mvn exec:java -Dexec.mainClass="ResponseFilter"```
5. Pro validní znovuspuštění je třeba kontrolovat adresář *src/main/checkpoint*, v případě chyb ho smazat
[text](README.md)


sudo /opt/maven/bin/mvn exec:java -Dexec.mainClass="cz.uhk.spark.aggregation.CountEmployeesInLabTest"