package com.example.order.client;

import java.util.List;

/**
 * Strategy interface for communicating with inventory-service.
 *
 * Strategy Pattern: OrderServiceImpl depends on this interface, not on the HTTP
 * implementation. In tests we inject a mock (or stub) — no real HTTP calls needed.
 * In production we use HttpInventoryClient.
 */
public interface InventoryClient {

    /**
     * Asks inventory-service to reserve stock for the given order.
     *
     * @param orderId   correlates the reservation back to the order
     * @param items     list of product+quantity pairs to reserve
     * @return reservationId  used later to release stock if the order is cancelled
     * @throws com.example.order.exception.InsufficientStockException if any item has
     *         insufficient available quantity
     */
    String reserveStock(String orderId, List<StockReservationItem> items);

    /**
     * Releases a previously made reservation (Saga compensation action).
     * Called when order creation fails after stock was already reserved.
     */
    void releaseStock(String reservationId);

    /** A product + quantity pair sent to inventory-service. */
    record StockReservationItem(String productId, int quantity) {}
}
