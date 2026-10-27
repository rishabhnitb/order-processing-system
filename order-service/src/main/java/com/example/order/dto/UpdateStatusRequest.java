package com.example.order.dto;

import com.example.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** Request body for PATCH /api/v1/orders/{id}/status */
public record UpdateStatusRequest(

        @NotNull(message = "status is required")
        OrderStatus status
) {}
