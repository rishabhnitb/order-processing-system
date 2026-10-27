package com.example.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReserveStockRequest(

        @NotBlank(message = "orderId is required")
        String orderId,

        @NotEmpty(message = "items must not be empty")
        @Valid
        List<StockItemRequest> items
) {
    public record StockItemRequest(
            @NotBlank(message = "productId is required")
            String productId,

            @Min(value = 1, message = "quantity must be at least 1")
            int quantity
    ) {}
}
