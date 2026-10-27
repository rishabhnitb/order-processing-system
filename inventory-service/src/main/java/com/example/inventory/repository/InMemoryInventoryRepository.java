package com.example.inventory.repository;

import com.example.inventory.domain.StockItem;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory inventory store pre-seeded with 4 products.
 * Same ConcurrentHashMap.compute() trick as InMemoryOrderRepository
 * to make reserve() and release() atomic.
 */
@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    private final ConcurrentHashMap<String, StockItem> store = new ConcurrentHashMap<>();

    public InMemoryInventoryRepository() {
        // Seed with products available at startup
        store.put("prod-1", new StockItem("prod-1", "Widget A", 100, 0));
        store.put("prod-2", new StockItem("prod-2", "Widget B", 50, 0));
        store.put("prod-3", new StockItem("prod-3", "Gadget X", 25, 0));
        store.put("prod-4", new StockItem("prod-4", "Gadget Y", 10, 0));
    }

    @Override
    public Optional<StockItem> findById(String productId) {
        return Optional.ofNullable(store.get(productId));
    }

    @Override
    public StockItem save(StockItem stockItem) {
        store.put(stockItem.productId(), stockItem);
        return stockItem;
    }

    @Override
    public List<StockItem> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean reserve(String productId, int quantity) {
        AtomicBoolean success = new AtomicBoolean(false);
        store.compute(productId, (key, existing) -> {
            if (existing != null && existing.hasEnough(quantity)) {
                success.set(true);
                return existing.withReserved(quantity);
            }
            return existing;
        });
        return success.get();
    }

    @Override
    public boolean release(String productId, int quantity) {
        AtomicBoolean success = new AtomicBoolean(false);
        store.compute(productId, (key, existing) -> {
            if (existing != null) {
                success.set(true);
                return existing.withReleased(quantity);
            }
            return existing;
        });
        return success.get();
    }
}
