package com.example.inventory.service;

import com.example.inventory.dto.ReserveStockRequest;
import com.example.inventory.dto.ReserveStockResponse;
import com.example.inventory.dto.StockResponse;

public interface InventoryService {

    /** Reserve stock for all items in the request (all-or-nothing with compensation). */
    ReserveStockResponse reserveStock(ReserveStockRequest request);

    /** Release a reservation — called on order cancellation (Saga compensation). */
    void releaseStock(String reservationId);

    /** Get current stock levels for a product. */
    StockResponse getStock(String productId);
}
