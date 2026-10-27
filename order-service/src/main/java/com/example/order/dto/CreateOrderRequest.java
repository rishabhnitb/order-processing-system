package com.example.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Request body for POST /api/v1/orders */
public record CreateOrderRequest(

        @NotBlank(message = "customerId is required")
        String customerId,

        // @NotEmpty: list must be non-null and have at least one element
        // @Valid: cascade validation into each OrderItemRequest
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderItemRequest> items
) {}
