# Build context must be the repo root (parent pom.xml is required by child modules)

# Stage 1 — build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
# Parent POM first so Maven can resolve the child module
COPY pom.xml ./pom.xml
# Both module pom.xml files needed so the parent can discover all sibling modules
COPY order-service/pom.xml ./order-service/pom.xml
COPY inventory-service/pom.xml ./inventory-service/pom.xml
# Only this service's sources
COPY order-service/src ./order-service/src
RUN --mount=type=cache,target=/root/.m2 \
    apk add --no-cache maven && \
    mvn -B -pl order-service -am package -DskipTests

# Stage 2 — runtime (JRE only, much smaller)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/order-service/target/order-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
