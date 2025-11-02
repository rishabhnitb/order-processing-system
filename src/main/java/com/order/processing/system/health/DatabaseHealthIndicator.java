package com.order.processing.system.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            // Test database connectivity
            int result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result == 1) {
                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connected")
                        .build();
            }
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Invalid response")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
