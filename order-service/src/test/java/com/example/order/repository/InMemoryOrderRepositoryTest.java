package com.example.order.repository;

import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InMemoryOrderRepository.
 *
 * The most important test here is the concurrency test — it proves that
 * ConcurrentHashMap.compute() makes transition() atomic under race conditions.
 *
 * Scenario: 20 threads simultaneously try to either promote PENDING→PROCESSING
 * or cancel PENDING→CANCELLED for the same order.
 * Exactly ONE must win. The rest must be silently rejected (return empty).
 */
class InMemoryOrderRepositoryTest {

    private InMemoryOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
    }

    @Test
    @DisplayName("save and findById round-trip")
    void saveAndFindById() {
        Order order = Order.create("cust-1", List.of(
                new OrderItem("p1", "Widget", 1, BigDecimal.TEN)
        ));
        repository.save(order);

        Optional<Order> found = repository.findById(order.id());
        assertThat(found).isPresent();
        assertThat(found.get().customerId()).isEqualTo("cust-1");
    }

    @Test
    @DisplayName("findByStatus filters correctly")
    void findByStatus_filtersCorrectly() {
        Order pending = Order.create("cust-1", List.of());
        Order processing = Order.create("cust-2", List.of()).withStatus(OrderStatus.PROCESSING);
        repository.save(pending);
        repository.save(processing);

        assertThat(repository.findByStatus(OrderStatus.PENDING)).hasSize(1);
        assertThat(repository.findByStatus(OrderStatus.PROCESSING)).hasSize(1);
        assertThat(repository.findByStatus(OrderStatus.SHIPPED)).isEmpty();
    }

    @Test
    @DisplayName("transition: returns updated order when current status matches expected")
    void transition_succeeds_whenStatusMatches() {
        Order order = Order.create("cust-1", List.of());
        repository.save(order);

        Optional<Order> result = repository.transition(
                order.id(), OrderStatus.PENDING, OrderStatus.PROCESSING);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("transition: returns empty when current status does not match expected")
    void transition_returnsEmpty_whenStatusMismatch() {
        Order order = Order.create("cust-1", List.of()).withStatus(OrderStatus.PROCESSING);
        repository.save(order);

        // Try to transition from PENDING → PROCESSING, but current is PROCESSING
        Optional<Order> result = repository.transition(
                order.id(), OrderStatus.PENDING, OrderStatus.PROCESSING);

        assertThat(result).isEmpty();
        // Status unchanged
        assertThat(repository.findById(order.id()).get().status()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("CONCURRENCY: exactly one thread wins when scheduler and cancel race on same order")
    void transition_isAtomic_underConcurrency() throws InterruptedException {
        // Create one PENDING order
        Order order = Order.create("cust-1", List.of(
                new OrderItem("p1", "Widget", 1, BigDecimal.TEN)
        ));
        repository.save(order);

        int threads = 20;
        CountDownLatch startGate = new CountDownLatch(1);   // holds all threads until we release
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger promotions = new AtomicInteger(0);
        AtomicInteger cancellations = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final boolean isScheduler = (i % 2 == 0); // even → scheduler, odd → cancel
            executor.submit(() -> {
                try {
                    startGate.await(); // all threads wait here together

                    if (isScheduler) {
                        repository.transition(order.id(), OrderStatus.PENDING, OrderStatus.PROCESSING)
                                .ifPresent(o -> promotions.incrementAndGet());
                    } else {
                        repository.transition(order.id(), OrderStatus.PENDING, OrderStatus.CANCELLED)
                                .ifPresent(o -> cancellations.incrementAndGet());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // release all 20 threads simultaneously
        doneLatch.await();
        executor.shutdown();

        // EXACTLY ONE thread must have won the transition
        int totalTransitions = promotions.get() + cancellations.get();
        assertThat(totalTransitions)
                .as("Exactly one thread must win the race (atomic check-and-set)")
                .isEqualTo(1);

        // Final state is consistent with whichever thread won
        Order finalOrder = repository.findById(order.id()).orElseThrow();
        assertThat(finalOrder.status()).isIn(OrderStatus.PROCESSING, OrderStatus.CANCELLED);
    }
}
