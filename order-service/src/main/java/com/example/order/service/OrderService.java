package com.example.order.service;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.CreateOrderRequest;

import java.util.List;
import java.util.UUID;

/**
 * Business operations for orders.
 * The controller depends on this interface — not on the implementation class.
 * Dependency Inversion Principle: high-level (controller) doesn't depend on low-level (impl).
 */
public interface OrderService {

    /** Create a new order and reserve stock via inventory-service (Saga step 1 + 2). */
    Order createOrder(CreateOrderRequest request);

    /** Fetch a single order, throws OrderNotFoundException if not found. */
    Order getOrder(UUID id);

    /** List all orders; if status is non-null, filter by that status. */
    List<Order> getAllOrders(OrderStatus status);

    /** Manually advance an order's status (validates the transition). */
    Order updateStatus(UUID id, OrderStatus newStatus);

    /** Cancel an order — only allowed when status is PENDING. */
    Order cancelOrder(UUID id);
}
