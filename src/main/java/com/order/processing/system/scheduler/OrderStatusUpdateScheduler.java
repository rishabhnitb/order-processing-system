package com.order.processing.system.scheduler;

import com.order.processing.system.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusUpdateScheduler {

    private final OrderService orderService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(fixedRate = 300000, initialDelay = 60000) // 5 minutes, with 1-minute initial delay
    public void updatePendingOrders() {
        String startTime = LocalDateTime.now().format(formatter);
        log.info("Starting scheduled update of pending orders at: {}", startTime);

        try {
            int updatedCount = orderService.updatePendingOrders();
            log.info("Successfully updated {} pending orders to PROCESSING at: {}",
                    updatedCount,
                    LocalDateTime.now().format(formatter));
        } catch (Exception e) {
            log.error("Error updating pending orders: {}", e.getMessage(), e);
        }
    }
}
