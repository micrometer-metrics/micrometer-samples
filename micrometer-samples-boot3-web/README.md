# Spring Boot 3 Web Sample

1. Start Prometheus Grafana, and Zipkin: `docker compose up` in this directory
2. Start the application: use your IDE or `./gradlew :micrometer-samples-boot3-web:bootRun` from the project root
3. Generate some load: `./generate-load.sh` in this directory

Backends:
- Zipkin: http://localhost:9411/zipkin/
- Prometheus: http://localhost:9090/
- Grafana: http://localhost:3000/
