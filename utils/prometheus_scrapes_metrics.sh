curl -G -fsS \
--data-urlencode 'query=avg((node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes) * 100' \ 
--data-urlencode 'start=1711565903' \
--data-urlencode 'end=1711485491' \
--data-urlencode 'step=1s' 'http://virtualserver1:9090/api/v1/query_range' > input.json

curl -G -fsS 
--data-urlencode 'query=(sum by (instance) (irate(node_cpu_seconds_total{mode!="idle"}[5s])) / on(instance) group_left sum by (instance) (irate(node_cpu_seconds_total[5s]))) * 100' \
--data-urlencode 'start=1711324800' \ 
--data-urlencode 'end=1711325098' \ 
--data-urlencode 'step=5s' 'http://virtualserver1:9090/api/v1/query_range' > input.json