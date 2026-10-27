package com.example.order.controller;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderItemRequest;
import com.example.order.dto.UpdateStatusRequest;
import com.example.order.exception.GlobalExceptionHandler;
import com.example.order.exception.InvalidStatusTransitionException;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests using @WebMvcTest — loads ONLY the web layer (controller, filters,
 * exception handlers). No full Spring context. Very fast.
 *
 * @MockBean replaces OrderService with a Mockito mock.
 * @Import(OrderMapper.class) adds the mapper bean that the controller needs.
 * GlobalExceptionHandler is a @RestControllerAdvice so it's auto-detected.
 */
@WebMvcTest(OrderController.class)
@Import({OrderMapper.class, GlobalExceptionHandler.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    // ── POST /api/v1/orders ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /orders: 201 Created with Location header")
    void createOrder_returns201() throws Exception {
        Order order = Order.create("cust-1", List.of());
        when(orderService.createOrder(any())).thenReturn(order);

        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of(
                new OrderItemRequest("prod-1", "Widget", 1, new BigDecimal("10.00"))
        ));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.customerId").value("cust-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /orders: 400 Bad Request when items list is empty")
    void createOrder_returns400_whenItemsEmpty() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /orders: 400 Bad Request when customerId is blank")
    void createOrder_returns400_whenCustomerIdBlank() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("", List.of(
                new OrderItemRequest("prod-1", "Widget", 1, new BigDecimal("10.00"))
        ));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/v1/orders/{id} ────────────────────────────────────────────

    @Test
    @DisplayName("GET /orders/{id}: 404 Not Found for unknown id")
    void getOrder_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrder(id)).thenThrow(new OrderNotFoundException(id));

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── POST /api/v1/orders/{id}/cancel ───────────────────────────────────

    @Test
    @DisplayName("POST /orders/{id}/cancel: 409 Conflict when status is not PENDING")
    void cancelOrder_returns409_whenNotCancellable() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.cancelOrder(id))
                .thenThrow(new InvalidStatusTransitionException(OrderStatus.SHIPPED, OrderStatus.CANCELLED));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid status transition"));
    }

    // ── PATCH /api/v1/orders/{id}/status ──────────────────────────────────

    @Test
    @DisplayName("PATCH /orders/{id}/status: 200 OK for valid transition")
    void updateStatus_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Order shipped = Order.create("cust-1", List.of()).withStatus(OrderStatus.SHIPPED);
        when(orderService.updateStatus(eq(id), eq(OrderStatus.SHIPPED))).thenReturn(shipped);

        UpdateStatusRequest request = new UpdateStatusRequest(OrderStatus.SHIPPED);

        mockMvc.perform(patch("/api/v1/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    // ── GET /api/v1/orders?status=PENDING ─────────────────────────────────

    @Test
    @DisplayName("GET /orders?status=PENDING: returns filtered list")
    void getAllOrders_withStatusFilter_returns200() throws Exception {
        Order pending = Order.create("cust-1", List.of());
        when(orderService.getAllOrders(OrderStatus.PENDING)).thenReturn(List.of(pending));

        mockMvc.perform(get("/api/v1/orders").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
