package com.example.inventory.controller;

import com.example.inventory.dto.ReleaseStockRequest;
import com.example.inventory.dto.ReserveStockRequest;
import com.example.inventory.dto.ReserveStockResponse;
import com.example.inventory.dto.StockResponse;
import com.example.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock reservation and release")
public class InventoryController {

    private final InventoryService inventoryService;

    /** Called by order-service during createOrder (Saga step 1). */
    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for an order")
    public ResponseEntity<ReserveStockResponse> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        return ResponseEntity.ok(inventoryService.reserveStock(request));
    }

    /** Called by order-service if order save fails (Saga compensation). */
    @PostMapping("/release")
    @Operation(summary = "Release a reservation")
    public ResponseEntity<Void> releaseStock(@Valid @RequestBody ReleaseStockRequest request) {
        inventoryService.releaseStock(request.reservationId());
        return ResponseEntity.noContent().build();
    }

    /** Check current stock levels for a product. */
    @GetMapping("/stock/{productId}")
    @Operation(summary = "Get stock level for a product")
    public ResponseEntity<StockResponse> getStock(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getStock(productId));
    }
}
