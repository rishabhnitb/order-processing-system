package com.order.processing.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service class implementing Spring Boot's HealthIndicator to provide custom health checks.
 * Monitors database connectivity and provides detailed health information.
 */
@Slf4j
@Component("healthCheck")
@RequiredArgsConstructor
public class HealthCheckService implements HealthIndicator {

    private final DataSource dataSource;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Performs health check and returns the current health status.
     * Checks database connectivity and provides detailed health information.
     *
     * @return Health object containing status and details
     */
    @Override
    public Health health() {
        try {
            checkDatabaseConnection();
            return Health.up()
                    .withDetail("timestamp", LocalDateTime.now().format(formatter))
                    .withDetail("database", "UP")
                    .build();
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("timestamp", LocalDateTime.now().format(formatter))
                    .withDetail("error", e.getMessage())
                    .withDetail("database", "DOWN")
                    .build();
        }
    }

    /**
     * Tests database connectivity by attempting to establish a connection.
     *
     * @throws Exception if database connection fails
     */
    private void checkDatabaseConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(3)) { // 3 seconds timeout
                throw new Exception("Database connection is invalid");
            }
        }
    }

    /**
     * Returns a formatted health status string for logging purposes.
     *
     * @return String containing current health status and timestamp
     */
    public String getHealthStatus() {
        Health health = health();
        return String.format("Application Status: %s, Timestamp: %s",
                health.getStatus(),
                health.getDetails().get("timestamp"));
    }
}
