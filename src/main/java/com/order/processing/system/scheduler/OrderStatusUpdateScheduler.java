package com.order.processing.system.scheduler;

import com.order.processing.system.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusUpdateScheduler {

    private final OrderService orderService;

    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void updatePendingOrders() {
        log.info("Starting scheduled update of pending orders");
        orderService.updatePendingOrders();
        log.info("Completed scheduled update of pending orders");
    }
}
