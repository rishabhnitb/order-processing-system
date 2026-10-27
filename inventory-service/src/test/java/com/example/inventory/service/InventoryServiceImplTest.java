package com.example.inventory.service;

import com.example.inventory.dto.ReserveStockRequest;
import com.example.inventory.dto.ReserveStockRequest.StockItemRequest;
import com.example.inventory.dto.ReserveStockResponse;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.exception.ReservationNotFoundException;
import com.example.inventory.repository.InMemoryInventoryRepository;
import com.example.inventory.repository.InMemoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InventoryServiceImplTest {

    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        // Fresh repositories per test — no shared state bleed
        inventoryService = new InventoryServiceImpl(
                new InMemoryInventoryRepository(),
                new InMemoryReservationRepository()
        );
    }

    @Test
    @DisplayName("reserveStock: reduces available quantity and returns a reservationId")
    void reserveStock_success() {
        ReserveStockRequest request = new ReserveStockRequest("order-1", List.of(
                new StockItemRequest("prod-1", 5)
        ));

        ReserveStockResponse response = inventoryService.reserveStock(request);

        assertThat(response.reservationId()).isNotBlank();
        // prod-1 started with 100; 5 were reserved → 95 available
        assertThat(inventoryService.getStock("prod-1").availableQuantity()).isEqualTo(95);
        assertThat(inventoryService.getStock("prod-1").reservedQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("reserveStock: throws InsufficientStockException when quantity exceeds available")
    void reserveStock_throwsWhenInsufficientStock() {
        ReserveStockRequest request = new ReserveStockRequest("order-1", List.of(
                new StockItemRequest("prod-4", 999)  // prod-4 has only 10
        ));

        assertThatThrownBy(() -> inventoryService.reserveStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("prod-4");
    }

    @Test
    @DisplayName("reserveStock: throws ProductNotFoundException for unknown product")
    void reserveStock_throwsWhenProductNotFound() {
        ReserveStockRequest request = new ReserveStockRequest("order-1", List.of(
                new StockItemRequest("non-existent-product", 1)
        ));

        assertThatThrownBy(() -> inventoryService.reserveStock(request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("non-existent-product");
    }

    @Test
    @DisplayName("releaseStock: restores available quantity after release")
    void releaseStock_restoresInventory() {
        // Reserve 10 units
        ReserveStockRequest request = new ReserveStockRequest("order-1", List.of(
                new StockItemRequest("prod-1", 10)
        ));
        ReserveStockResponse reservation = inventoryService.reserveStock(request);
        assertThat(inventoryService.getStock("prod-1").availableQuantity()).isEqualTo(90);

        // Release the reservation
        inventoryService.releaseStock(reservation.reservationId());

        // Stock should be back to 100
        assertThat(inventoryService.getStock("prod-1").availableQuantity()).isEqualTo(100);
        assertThat(inventoryService.getStock("prod-1").reservedQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("releaseStock: throws ReservationNotFoundException for unknown reservationId")
    void releaseStock_throwsWhenReservationNotFound() {
        assertThatThrownBy(() -> inventoryService.releaseStock("non-existent-reservation"))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @DisplayName("reserveStock: multiple items reserved atomically")
    void reserveStock_multipleItems() {
        ReserveStockRequest request = new ReserveStockRequest("order-1", List.of(
                new StockItemRequest("prod-1", 10),
                new StockItemRequest("prod-2", 5)
        ));

        ReserveStockResponse response = inventoryService.reserveStock(request);

        assertThat(response.reservationId()).isNotBlank();
        assertThat(inventoryService.getStock("prod-1").availableQuantity()).isEqualTo(90);
        assertThat(inventoryService.getStock("prod-2").availableQuantity()).isEqualTo(45);
    }

    @Test
    @DisplayName("getStock: throws ProductNotFoundException for unknown product")
    void getStock_throwsForUnknownProduct() {
        assertThatThrownBy(() -> inventoryService.getStock("unknown"))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
