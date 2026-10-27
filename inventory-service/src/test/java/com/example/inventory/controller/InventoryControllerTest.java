package com.example.inventory.controller;

import com.example.inventory.dto.ReleaseStockRequest;
import com.example.inventory.dto.ReserveStockRequest;
import com.example.inventory.dto.ReserveStockRequest.StockItemRequest;
import com.example.inventory.dto.ReserveStockResponse;
import com.example.inventory.dto.StockResponse;
import com.example.inventory.exception.GlobalExceptionHandler;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import(GlobalExceptionHandler.class)
class InventoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean InventoryService inventoryService;

    @Test
    @DisplayName("POST /reserve: 200 OK with reservationId")
    void reserveStock_returns200() throws Exception {
        when(inventoryService.reserveStock(any())).thenReturn(new ReserveStockResponse("res-abc"));

        ReserveStockRequest request = new ReserveStockRequest("order-1", List.of(
                new StockItemRequest("prod-1", 5)
        ));

        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value("res-abc"));
    }

    @Test
    @DisplayName("POST /reserve: 422 when insufficient stock")
    void reserveStock_returns422_whenInsufficientStock() throws Exception {
        when(inventoryService.reserveStock(any()))
                .thenThrow(new InsufficientStockException("prod-4", 999, 10));

        ReserveStockRequest request = new ReserveStockRequest("order-1", List.of(
                new StockItemRequest("prod-4", 999)
        ));

        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient stock"));
    }

    @Test
    @DisplayName("POST /release: 204 No Content on success")
    void releaseStock_returns204() throws Exception {
        doNothing().when(inventoryService).releaseStock(anyString());

        ReleaseStockRequest request = new ReleaseStockRequest("res-abc");

        mockMvc.perform(post("/api/v1/inventory/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /stock/{productId}: 200 OK with stock levels")
    void getStock_returns200() throws Exception {
        when(inventoryService.getStock("prod-1"))
                .thenReturn(new StockResponse("prod-1", "Widget A", 95, 5));

        mockMvc.perform(get("/api/v1/inventory/stock/prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(95))
                .andExpect(jsonPath("$.reservedQuantity").value(5));
    }

    @Test
    @DisplayName("GET /stock/{productId}: 404 for unknown product")
    void getStock_returns404_whenProductNotFound() throws Exception {
        when(inventoryService.getStock("unknown"))
                .thenThrow(new ProductNotFoundException("unknown"));

        mockMvc.perform(get("/api/v1/inventory/stock/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"));
    }
}
