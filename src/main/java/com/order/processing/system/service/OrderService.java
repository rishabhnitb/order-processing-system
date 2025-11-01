package com.order.processing.system.service;

import com.order.processing.system.dto.CreateOrderRequest;
import com.order.processing.system.dto.OrderResponse;
import com.order.processing.system.model.*;
import com.order.processing.system.repository.CustomerRepository;
import com.order.processing.system.repository.ItemRepository;
import com.order.processing.system.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Order order = new Order();
        order.setCustomer(customer);

        for (var itemRequest : request.getItems()) {
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
            .toList();
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
    public int updatePendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            return 0;
        }

        pendingOrders.forEach(order -> {
            order.setStatus(OrderStatus.PROCESSING);
            order.setUpdatedAt(LocalDateTime.now());
        });
        orderRepository.saveAll(pendingOrders);
        return pendingOrders.size();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        // Map customer details
        OrderResponse.CustomerDTO customerDTO = new OrderResponse.CustomerDTO();
        customerDTO.setId(order.getCustomer().getId());
        customerDTO.setName(order.getCustomer().getName());
        customerDTO.setEmail(order.getCustomer().getEmail());
        response.setCustomer(customerDTO);

        List<OrderResponse.OrderItemDTO> itemDTOs = order.getItems().stream()
            .map(this::mapToOrderItemDTO)
            .toList();

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
