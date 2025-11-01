package com.order.processing.system.service;

import com.order.processing.system.dto.OrderItemRequest;
import com.order.processing.system.dto.OrderResponse;
import com.order.processing.system.model.Item;
import com.order.processing.system.model.Order;
import com.order.processing.system.model.OrderStatus;
import com.order.processing.system.repository.ItemRepository;
import com.order.processing.system.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private OrderService orderService;

    private Item testItem;
    private Order testOrder;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Test Item");
        testItem.setPrice(10.0);

        testOrder = new Order();
        testOrder.setId(orderId);
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Test
    void createOrder_Success() {
        // Arrange
        OrderItemRequest request = new OrderItemRequest();
        request.setItemId(1L);
        request.setQuantity(2);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.createOrder(List.of(request));

        // Assert
        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void cancelOrder_WhenPending_Success() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.cancelOrder(orderId);

        // Assert
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void cancelOrder_WhenNotPending_ThrowsException() {
        // Arrange
        testOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(orderId));
    }

    @Test
    void updatePendingOrders_Success() {
        // Arrange
        List<Order> pendingOrders = List.of(testOrder);
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(pendingOrders);

        // Act
        orderService.updatePendingOrders();

        // Assert
        assertEquals(OrderStatus.PROCESSING, testOrder.getStatus());
        verify(orderRepository).saveAll(pendingOrders);
    }
}
