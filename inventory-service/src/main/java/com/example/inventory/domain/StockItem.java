package com.example.inventory.domain;

/**
 * Represents the stock level for a single product.
 * Immutable record — withReserved() and withReleased() return new instances.
 *
 * availableQuantity = units that can still be reserved
 * reservedQuantity  = units reserved but not yet consumed (order in flight)
 *
 * When an order is placed: available ↓, reserved ↑
 * When an order is cancelled: available ↑, reserved ↓
 */
public record StockItem(
        String productId,
        String productName,
        int availableQuantity,
        int reservedQuantity
) {

    public boolean hasEnough(int quantity) {
        return availableQuantity >= quantity;
    }

    public StockItem withReserved(int quantity) {
        return new StockItem(productId, productName,
                availableQuantity - quantity,
                reservedQuantity + quantity);
    }

    public StockItem withReleased(int quantity) {
        return new StockItem(productId, productName,
                availableQuantity + quantity,
                Math.max(0, reservedQuantity - quantity));
    }
}
