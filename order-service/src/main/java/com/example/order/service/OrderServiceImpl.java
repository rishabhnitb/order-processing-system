package com.example.order.service;

import com.example.order.client.InventoryClient;
import com.example.order.client.InventoryClient.StockReservationItem;
import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.exception.InvalidStatusTransitionException;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Core business logic for order management.
 *
 * Key design decisions:
 *
 * 1. Saga pattern in createOrder():
 *    Step 1 — reserve stock in inventory-service
 *    Step 2 — persist the order
 *    If step 2 fails → compensation: release the reservation
 *    This gives us best-effort consistency without a distributed transaction.
 *
 * 2. Atomic status transitions via repository.transition():
 *    Both cancelOrder() and the scheduler call transition() which uses
 *    ConcurrentHashMap.compute() — only one wins, the other gets empty back.
 *
 * 3. Facade pattern:
 *    The controller calls one method here and gets back a domain object.
 *    All coordination (inventory, persistence, state validation) is hidden here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderMapper orderMapper;

    @Override
    public Order createOrder(CreateOrderRequest request) {
        List<OrderItem> items = orderMapper.toOrderItems(request.items());
        Order order = Order.create(request.customerId(), items);

        // ── Saga Step 1: reserve stock ─────────────────────────────────────
        List<StockReservationItem> reservationItems = items.stream()
                .map(item -> new StockReservationItem(item.productId(), item.quantity()))
                .toList();

        String reservationId;
        try {
            reservationId = inventoryClient.reserveStock(order.id().toString(), reservationItems);
            log.info("Stock reserved: reservationId={} for orderId={}", reservationId, order.id());
        } catch (Exception ex) {
            // Stock reservation failed — order object was never saved, nothing to compensate
            log.error("Stock reservation failed for orderId={}: {}", order.id(), ex.getMessage());
            throw ex;
        }

        // ── Saga Step 2: persist the order ────────────────────────────────
        try {
            Order saved = orderRepository.save(order);
            log.info("Order created: id={}, customer={}, total={}", saved.id(), saved.customerId(), saved.totalAmount());
            return saved;
        } catch (Exception ex) {
            // Compensation: order save failed, so release the stock we just reserved
            log.error("Order save failed for orderId={} — releasing reservation={}", order.id(), reservationId);
            try {
                inventoryClient.releaseStock(reservationId);
            } catch (Exception compensationEx) {
                log.error("Compensation also failed — manual intervention needed for reservationId={}", reservationId);
            }
            throw ex;
        }
    }

    @Override
    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public List<Order> getAllOrders(OrderStatus status) {
        if (status != null) {
            return orderRepository.findByStatus(status);
        }
        return orderRepository.findAll();
    }

    @Override
    public Order updateStatus(UUID id, OrderStatus newStatus) {
        Order current = getOrder(id);

        if (!current.status().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(current.status(), newStatus);
        }

        // transition() is atomic — if another thread changed the status between
        // getOrder() and here, we get empty back and throw instead of corrupting state
        return orderRepository.transition(id, current.status(), newStatus)
                .orElseThrow(() -> new InvalidStatusTransitionException(current.status(), newStatus));
    }

    @Override
    public Order cancelOrder(UUID id) {
        Order current = getOrder(id);

        if (!current.status().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidStatusTransitionException(current.status(), OrderStatus.CANCELLED);
        }

        return orderRepository.transition(id, current.status(), OrderStatus.CANCELLED)
                .orElseThrow(() -> new InvalidStatusTransitionException(current.status(), OrderStatus.CANCELLED));
    }
}
