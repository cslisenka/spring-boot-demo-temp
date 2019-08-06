1. Prometheus
1.1 Allow docker to access host IP address
iptables -A INPUT -i docker0 -j ACCEPT

1.2 Get docker network interfact
ip addr show docker0
in my case it was "172.17.0.1"

1.3 Run prometheus
docker run -p 9090:9090 -v /media/kslisestorage/work/spring-boot-demo/spring-boot-demo/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus

2. Grafana
2.1 Start
docker run -d --name=grafana -p 3000:3000 grafana/grafana

2.2 Login
admin/admin

3.3 Set Prometheus datasource

3.4 Import dashboard
