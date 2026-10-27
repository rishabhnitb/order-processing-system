package com.example.order.controller;

import com.example.order.domain.OrderStatus;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.UpdateStatusRequest;
import com.example.order.mapper.OrderMapper;
import com.example.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller — HTTP concerns only.
 *
 * Responsibilities:
 *   - Map HTTP verbs + paths to service calls
 *   - Validate incoming request bodies with @Valid
 *   - Set correct HTTP status codes and Location headers
 *   - Convert domain objects to response DTOs via OrderMapper
 *
 * It does NOT contain any business logic — that lives in OrderServiceImpl.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order lifecycle management")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    /**
     * POST /api/v1/orders
     * Returns 201 Created with Location: /api/v1/orders/{id}
     */
    @PostMapping
    @Operation(summary = "Place a new order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var order = orderService.createOrder(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(order.id())
                .toUri();

        return ResponseEntity.created(location).body(orderMapper.toResponse(order));
    }

    /**
     * GET /api/v1/orders/{id}
     * Returns 200 OK, or 404 if not found (handled by GlobalExceptionHandler).
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderMapper.toResponse(orderService.getOrder(id)));
    }

    /**
     * GET /api/v1/orders?status=PENDING
     * status parameter is optional — omit to get all orders.
     */
    @GetMapping
    @Operation(summary = "List all orders, optionally filtered by status")
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status) {

        List<OrderResponse> responses = orderService.getAllOrders(status)
                .stream()
                .map(orderMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * PATCH /api/v1/orders/{id}/status
     * Manually update status — 409 Conflict if the transition is not allowed.
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {

        return ResponseEntity.ok(orderMapper.toResponse(orderService.updateStatus(id, request.status())));
    }

    /**
     * POST /api/v1/orders/{id}/cancel
     * Cancels the order — 409 Conflict if not in PENDING status.
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (only allowed when PENDING)")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderMapper.toResponse(orderService.cancelOrder(id)));
    }
}
