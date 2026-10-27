package com.example.order.repository;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory implementation of OrderRepository backed by a ConcurrentHashMap.
 *
 * Why ConcurrentHashMap?
 *   Multiple threads can read/write concurrently without locking the whole map.
 *   Individual key operations like compute() lock only a single bucket (hash bin),
 *   giving us fine-grained locking without explicit synchronized blocks.
 *
 * Why compute() in transition()?
 *   The naive approach (get → check → put) is NOT atomic: the scheduler can promote
 *   PENDING→PROCESSING between our check and our put, so a cancel would wrongly succeed.
 *   compute() holds the lock on that key's bin for the entire lambda, making the
 *   check-and-update a single atomic step.
 *
 * Production note: replace this class with JpaOrderRepository — no other code changes needed.
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final ConcurrentHashMap<UUID, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.id(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return store.values().stream()
                .filter(order -> order.status() == status)
                .toList();
    }

    /**
     * Atomic check-and-update using ConcurrentHashMap.compute().
     *
     * compute() acquires the per-key lock before invoking the lambda, so no two threads
     * can run the lambda for the same orderId at the same time.
     *
     * Inside the lambda:
     *   - If the order exists AND its current status equals expected → update and record result.
     *   - Otherwise → return existing unchanged (another thread already transitioned it).
     */
    @Override
    public Optional<Order> transition(UUID id, OrderStatus expected, OrderStatus next) {
        AtomicReference<Order> result = new AtomicReference<>();

        store.compute(id, (key, existing) -> {
            if (existing != null && existing.status() == expected) {
                Order updated = existing.withStatus(next);
                result.set(updated);
                return updated;
            }
            return existing; // no-op: wrong status or order doesn't exist
        });

        return Optional.ofNullable(result.get());
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
