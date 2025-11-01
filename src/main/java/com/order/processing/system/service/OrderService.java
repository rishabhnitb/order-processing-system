package com.order.processing.system.service;

import com.order.processing.system.dto.OrderItemRequest;
import com.order.processing.system.dto.OrderResponse;
import com.order.processing.system.model.Item;
import com.order.processing.system.model.Order;
import com.order.processing.system.model.OrderItem;
import com.order.processing.system.model.OrderStatus;
import com.order.processing.system.repository.ItemRepository;
import com.order.processing.system.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public OrderResponse createOrder(List<OrderItemRequest> orderItems) {
        Order order = new Order();

        for (OrderItemRequest itemRequest : orderItems) {
            Item item = itemRepository.findById(itemRequest.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + itemRequest.getItemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setItem(item);
            orderItem.setQuantity(itemRequest.getQuantity());
            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(OrderStatus status) {
        List<Order> orders = status == null ?
            orderRepository.findAll() :
            orderRepository.findByStatus(status);

        return orders.stream()
            .map(this::mapToOrderResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order can only be cancelled when in PENDING status");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return mapToOrderResponse(orderRepository.save(order));
    }

    @Transactional
    public void updatePendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        pendingOrders.forEach(order -> order.setStatus(OrderStatus.PROCESSING));
        orderRepository.saveAll(pendingOrders);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        List<OrderResponse.OrderItemDTO> itemDTOs = order.getItems().stream()
            .map(this::mapToOrderItemDTO)
            .collect(Collectors.toList());

        response.setItems(itemDTOs);
        response.setTotalAmount(itemDTOs.stream()
            .mapToDouble(OrderResponse.OrderItemDTO::getSubtotal)
            .sum());

        return response;
    }

    private OrderResponse.OrderItemDTO mapToOrderItemDTO(OrderItem orderItem) {
        OrderResponse.OrderItemDTO dto = new OrderResponse.OrderItemDTO();
        dto.setItemId(orderItem.getItem().getId());
        dto.setItemName(orderItem.getItem().getName());
        dto.setItemPrice(orderItem.getItem().getPrice());
        dto.setQuantity(orderItem.getQuantity());
        dto.setSubtotal(orderItem.getItem().getPrice() * orderItem.getQuantity());
        return dto;
    }
}
