package com.order.processing.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotEmpty(message = "Order must contain at least one item")
    private List<@Valid OrderItemRequest> items;
}
