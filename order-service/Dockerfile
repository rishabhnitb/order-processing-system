# Multi-stage build: keeps the final image small (no JDK or Maven in production)

# Stage 1 — build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
# Package the fat JAR (tests already passed in CI — skip here)
RUN --mount=type=cache,target=/root/.m2 \
    apk add --no-cache maven && \
    mvn -B package -DskipTests

# Stage 2 — runtime (JRE only, much smaller)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/order-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
