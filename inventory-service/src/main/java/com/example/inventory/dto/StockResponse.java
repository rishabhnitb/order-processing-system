package com.example.inventory.dto;

public record StockResponse(
        String productId,
        String productName,
        int availableQuantity,
        int reservedQuantity
) {}
