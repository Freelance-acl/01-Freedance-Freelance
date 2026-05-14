# Build context must be the repository root (see docker-compose.yaml).
FROM eclipse-temurin:25.0.2_10-jdk-noble AS build
WORKDIR /build
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY user-service user-service
COPY job-service job-service
COPY proposal-service proposal-service
COPY contract-service contract-service
COPY wallet-service wallet-service
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
    && ./mvnw -B -ntp -pl contract-service -am package -DskipTests

FROM eclipse-temurin:25.0.2_10-jre-noble
WORKDIR /app
RUN groupadd --system appgroup && useradd --system --gid appgroup --create-home appuser
COPY --from=build /build/contract-service/target/*-exec.jar app.jar
RUN chown -R appuser:appgroup /app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
USER appuser
CMD ["java", "-jar", "app.jar"]
