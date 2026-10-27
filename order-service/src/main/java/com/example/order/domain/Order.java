package com.example.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core domain object. Immutable Java record.
 *
 * Factory method pattern: Order.create() is the only way to construct a valid new Order.
 * The constructor is the record's canonical constructor — it is package-visible via the
 * record itself, but business code should always use Order.create() or withStatus().
 *
 * Immutability: withStatus() returns a NEW Order with the updated field — it never
 * mutates the original. This guarantees that code holding a reference to an Order
 * cannot change what the repository stored.
 */
public record Order(
        UUID id,
        String customerId,
        List<OrderItem> items,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt,
        String reservationId   // set after inventory reservation; needed to release stock on cancel
) {

    /**
     * Factory method — creates a brand-new PENDING order.
     * Calculates totalAmount from items so callers never pass it in directly.
     * reservationId is null until inventory confirms the reservation.
     */
    public static Order create(String customerId, List<OrderItem> items) {
        BigDecimal total = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant now = Instant.now();
        return new Order(
                UUID.randomUUID(),
                customerId,
                List.copyOf(items),   // defensive copy — caller cannot mutate items
                OrderStatus.PENDING,
                total,
                now,
                now,
                null
        );
    }

    /** Returns a copy of this order with the reservation ID attached. Called after Saga step 1. */
    public Order withReservationId(String newReservationId) {
        return new Order(id, customerId, items, status, totalAmount, createdAt, updatedAt, newReservationId);
    }

    /**
     * Returns a copy of this order with a new status and updated updatedAt timestamp.
     * Carries the reservationId through so cancel can still release stock.
     * Used by the repository's atomic transition method.
     */
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, customerId, items, newStatus, totalAmount, createdAt, Instant.now(), reservationId);
    }
}
