package com.order.processing.system.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
public class HealthCheckScheduler {

    @Autowired
    private Map<String, HealthIndicator> healthIndicators;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void checkHealth() {
        log.info("Performing scheduled health check at: {}", LocalDateTime.now().format(formatter));

        healthIndicators.forEach((name, indicator) -> {
            try {
                Health health = indicator.health();
                log.info("Health Indicator [{}] Status: {}", name, health.getStatus());
                if (!health.getStatus().equals(Health.up().build().getStatus())) {
                    log.warn("Health check failed for {}: {}", name, health.getDetails());
                }
            } catch (Exception e) {
                log.error("Error checking health indicator {}: {}", name, e.getMessage(), e);
            }
        });
    }
}
