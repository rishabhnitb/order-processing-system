package com.example.inventory.domain;

import java.util.List;
import java.util.UUID;

/**
 * Represents a stock reservation tied to an order.
 * Stored so we can release the exact quantities when a reservation is rolled back.
 */
public record Reservation(
        String reservationId,
        String orderId,
        List<ReservationItem> items
) {

    public static Reservation create(String orderId, List<ReservationItem> items) {
        return new Reservation(UUID.randomUUID().toString(), orderId, List.copyOf(items));
    }

    public record ReservationItem(String productId, int quantity) {}
}
