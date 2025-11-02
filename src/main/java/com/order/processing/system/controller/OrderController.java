package com.order.processing.system.controller;

import com.order.processing.system.dto.CreateOrderRequest;
import com.order.processing.system.dto.OrderResponse;
import com.order.processing.system.model.OrderStatus;
import com.order.processing.system.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing order operations in the e-commerce system.
 * Provides endpoints for creating, retrieving, and managing orders.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates a new order in the system.
     *
     * @param request The order creation request containing customer ID and items
     * @return ResponseEntity containing the created order details
     * @throws EntityNotFoundException if customer or items are not found
     */
    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    /**
     * Retrieves order details by its unique identifier.
     *
     * @param id The UUID of the order to retrieve
     * @return ResponseEntity containing the order details
     * @throws EntityNotFoundException if order is not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    /**
     * Retrieves all orders, optionally filtered by status.
     *
     * @param status Optional order status to filter results
     * @return ResponseEntity containing list of orders matching the criteria
     */
    @GetMapping
    @Operation(summary = "Get all orders, optionally filtered by status")
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getAllOrders(status));
    }

    /**
     * Cancels an existing order if it's in PENDING status.
     *
     * @param id The UUID of the order to cancel
     * @return ResponseEntity containing the updated order details
     * @throws EntityNotFoundException if order is not found
     * @throws IllegalStateException if order is not in PENDING status
     */
    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
