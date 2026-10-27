package com.example.order.repository;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface — the only abstraction between the service layer and storage.
 *
 * Repository Pattern: the service never knows whether data lives in a ConcurrentHashMap,
 * a PostgreSQL database, or anything else. To add JPA support, add JpaOrderRepository
 * implementing this interface — zero changes to OrderServiceImpl.
 *
 * The key method is transition(): it is designed to be atomic, handling the race
 * condition where the scheduler and a cancel request hit the same order simultaneously.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    List<Order> findAll();

    List<Order> findByStatus(OrderStatus status);

    /**
     * Atomically checks that the order's current status equals {@code expected},
     * then updates it to {@code next}.
     *
     * Returns the updated Order if the transition happened, or empty if the order
     * was not in the expected status (lost race condition or wrong state).
     *
     * This is the single point of concurrency control for status changes.
     */
    Optional<Order> transition(UUID id, OrderStatus expected, OrderStatus next);

    /** Clears all data — used in integration tests only. */
    void deleteAll();
}
