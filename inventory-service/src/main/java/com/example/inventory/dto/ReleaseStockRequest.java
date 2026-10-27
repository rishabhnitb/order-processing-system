package com.example.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record ReleaseStockRequest(
        @NotBlank(message = "reservationId is required")
        String reservationId
) {}
