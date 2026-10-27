package com.example.order.scheduler;

import com.example.order.domain.OrderStatus;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background job: every N milliseconds, promote all PENDING orders to PROCESSING.
 *
 * Why this is safe under concurrency:
 *   For each PENDING order it calls repository.transition(id, PENDING, PROCESSING).
 *   If a customer cancelled that order between findByStatus() and transition(),
 *   transition() returns empty (the status no longer matches PENDING) — the scheduler
 *   silently skips it. No corrupt state, no exception.
 *
 * Interval is configurable via application.yml / env var:
 *   orders.scheduler.interval-ms=300000  (default: 5 minutes)
 *   SCHEDULER_INTERVAL_MS=10000          (docker-compose demo: 10 seconds)
 *
 * Production note: with multiple instances you'd add ShedLock to ensure exactly
 * one instance runs this job at a time (not both).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;

    @Scheduled(fixedRateString = "${orders.scheduler.interval-ms:300000}")
    public void promotePendingOrders() {
        log.info("Scheduler run: promoting PENDING → PROCESSING");

        orderRepository.findByStatus(OrderStatus.PENDING).forEach(order -> {
            orderRepository.transition(order.id(), OrderStatus.PENDING, OrderStatus.PROCESSING)
                    .ifPresent(updated ->
                            log.info("Scheduler: promoted order {} to PROCESSING", updated.id()));
        });
    }
}
