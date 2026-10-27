package com.example.order.dto;

import com.example.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API response for any order endpoint.
 * Nested record for items keeps the DTO layer self-contained.
 */
public record OrderResponse(
        UUID id,
        String customerId,
        List<OrderItemResponse> items,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt
) {
    public record OrderItemResponse(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
