package com.example.inventory.repository;

import com.example.inventory.domain.StockItem;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    Optional<StockItem> findById(String productId);
    StockItem save(StockItem stockItem);
    List<StockItem> findAll();

    /**
     * Atomically check-and-reserve: decrements availableQuantity by quantity
     * if enough stock exists.
     * @return true if reservation succeeded, false if insufficient stock (lost race)
     */
    boolean reserve(String productId, int quantity);

    /**
     * Atomically releases previously reserved stock back to available.
     * @return true if release succeeded
     */
    boolean release(String productId, int quantity);
}
