# Build stage
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/processing.system-0.0.1-SNAPSHOT.jar app.jar

# Default to dev profile if not specified
ENV SPRING_PROFILES_ACTIVE=dev

EXPOSE 8080

# Add JVM options for containerized environment
ENTRYPOINT ["java", \
    "-jar", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", \
    "app.jar"]
