package com.example.order.service;

import com.example.order.client.InventoryClient;
import com.example.order.client.InventoryClient.StockReservationItem;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderItemRequest;
import com.example.order.exception.InsufficientStockException;
import com.example.order.exception.InvalidStatusTransitionException;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl.
 *
 * We mock OrderRepository and InventoryClient — no Spring context loaded,
 * so these tests are extremely fast.
 * OrderMapper is instantiated directly (it's stateless, no reason to mock it).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, inventoryClient, new OrderMapper());
    }

    // ── createOrder ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOrder: happy path — reserves stock then saves order")
    void createOrder_success() {
        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of(
                new OrderItemRequest("prod-1", "Widget", 2, new BigDecimal("10.00"))
        ));
        when(inventoryClient.reserveStock(anyString(), anyList())).thenReturn("res-123");
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.createOrder(request);

        assertThat(result.customerId()).isEqualTo("cust-1");
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.totalAmount()).isEqualByComparingTo("20.00");  // 2 × 10.00

        verify(inventoryClient).reserveStock(anyString(), anyList());
        verify(orderRepository).save(any());
    }

    @Test
    @DisplayName("createOrder: releases stock (compensation) when order save fails")
    void createOrder_compensatesWhenSaveFails() {
        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of(
                new OrderItemRequest("prod-1", "Widget", 1, new BigDecimal("10.00"))
        ));
        when(inventoryClient.reserveStock(anyString(), anyList())).thenReturn("res-123");
        when(orderRepository.save(any())).thenThrow(new RuntimeException("storage unavailable"));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(RuntimeException.class);

        // Saga compensation must fire — stock was reserved so it must be released
        verify(inventoryClient).releaseStock("res-123");
    }

    @Test
    @DisplayName("createOrder: throws InsufficientStockException and never touches repository")
    void createOrder_failsWhenInsufficientStock() {
        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of(
                new OrderItemRequest("prod-1", "Widget", 999, new BigDecimal("10.00"))
        ));
        when(inventoryClient.reserveStock(anyString(), anyList()))
                .thenThrow(new InsufficientStockException("Not enough stock for prod-1"));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("prod-1");

        verify(orderRepository, never()).save(any());
    }

    // ── cancelOrder ────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelOrder: succeeds when order is PENDING")
    void cancelOrder_success_whenPending() {
        UUID id = UUID.randomUUID();
        Order pending = Order.create("cust-1", List.of());
        Order cancelled = pending.withStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(id)).thenReturn(Optional.of(pending));
        when(orderRepository.transition(any(), eq(OrderStatus.PENDING), eq(OrderStatus.CANCELLED)))
                .thenReturn(Optional.of(cancelled));

        Order result = orderService.cancelOrder(id);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelOrder: throws InvalidStatusTransitionException when order is SHIPPED")
    void cancelOrder_fails_whenShipped() {
        UUID id = UUID.randomUUID();
        Order shipped = Order.create("cust-1", List.of()).withStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(id)).thenReturn(Optional.of(shipped));

        assertThatThrownBy(() -> orderService.cancelOrder(id))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("SHIPPED");
    }

    @Test
    @DisplayName("cancelOrder: throws InvalidStatusTransitionException when order is DELIVERED")
    void cancelOrder_fails_whenDelivered() {
        UUID id = UUID.randomUUID();
        Order delivered = Order.create("cust-1", List.of()).withStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(id)).thenReturn(Optional.of(delivered));

        assertThatThrownBy(() -> orderService.cancelOrder(id))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ── getOrder ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrder: throws OrderNotFoundException for unknown id")
    void getOrder_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(id))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── updateStatus ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: PROCESSING → SHIPPED succeeds")
    void updateStatus_validTransition_succeeds() {
        UUID id = UUID.randomUUID();
        Order processing = Order.create("cust-1", List.of()).withStatus(OrderStatus.PROCESSING);
        Order shipped = processing.withStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(id)).thenReturn(Optional.of(processing));
        when(orderRepository.transition(any(), eq(OrderStatus.PROCESSING), eq(OrderStatus.SHIPPED)))
                .thenReturn(Optional.of(shipped));

        Order result = orderService.updateStatus(id, OrderStatus.SHIPPED);

        assertThat(result.status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("updateStatus: DELIVERED → PENDING throws (terminal state)")
    void updateStatus_invalidTransition_throws() {
        UUID id = UUID.randomUUID();
        Order delivered = Order.create("cust-1", List.of()).withStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(id)).thenReturn(Optional.of(delivered));

        assertThatThrownBy(() -> orderService.updateStatus(id, OrderStatus.PENDING))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("DELIVERED");
    }

    // ── getAllOrders ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllOrders: null status → returns all")
    void getAllOrders_noFilter_returnsAll() {
        when(orderRepository.findAll()).thenReturn(List.of(
                Order.create("cust-1", List.of()),
                Order.create("cust-2", List.of())
        ));

        assertThat(orderService.getAllOrders(null)).hasSize(2);
        verify(orderRepository).findAll();
        verify(orderRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("getAllOrders: status filter delegates to findByStatus")
    void getAllOrders_withFilter_delegatesToFindByStatus() {
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(
                Order.create("cust-1", List.of())
        ));

        assertThat(orderService.getAllOrders(OrderStatus.PENDING)).hasSize(1);
        verify(orderRepository).findByStatus(OrderStatus.PENDING);
    }
}
