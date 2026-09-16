ARG DOCKER_REGISTRY
FROM ${DOCKER_REGISTRY:-docker.io}/maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn dependency:go-offline -Dmaven.repo.local=/root/.m2/repository -Dmaven.wagon.http.retryHandler.count=10
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.repo.local=/root/.m2/repository -Dmaven.wagon.http.retryHandler.count=10

FROM ${DOCKER_REGISTRY:-docker.io}/eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]