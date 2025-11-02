package com.order.processing.system.repository;

import com.order.processing.system.model.Order;
import com.order.processing.system.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Order entity operations.
 * Extends JpaRepository to provide standard CRUD operations and custom query methods.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Finds all orders with the specified status.
     *
     * @param status The order status to filter by
     * @return List of orders matching the status
     */
    List<Order> findByStatus(OrderStatus status);
}
