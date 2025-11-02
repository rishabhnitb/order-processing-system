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

/**
 * Service class for managing order-related business logic.
 * Handles order creation, retrieval, cancellation, and status updates.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final CustomerRepository customerRepository;

    /**
     * Creates a new order for a customer with specified items.
     *
     * @param request The order creation request containing customer ID and items
     * @return OrderResponse containing the created order details
     * @throws EntityNotFoundException if customer or any item is not found
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Find customer or throw exception if not found
        Customer customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Order order = new Order();
        order.setCustomer(customer);

        // Process each item in the order
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

    /**
     * Retrieves order details by ID.
     *
     * @param id The UUID of the order to retrieve
     * @return OrderResponse containing the order details
     * @throws EntityNotFoundException if order is not found
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        return mapToOrderResponse(order);
    }

    /**
     * Retrieves all orders, optionally filtered by status.
     *
     * @param status Optional order status to filter results
     * @return List of OrderResponse objects matching the criteria
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(OrderStatus status) {
        List<Order> orders = status == null ?
            orderRepository.findAll() :
            orderRepository.findByStatus(status);

        return orders.stream()
            .map(this::mapToOrderResponse)
            .toList();
    }

    /**
     * Cancels an order if it's in PENDING status.
     *
     * @param id The UUID of the order to cancel
     * @return OrderResponse containing the updated order details
     * @throws EntityNotFoundException if order is not found
     * @throws IllegalStateException if order is not in PENDING status
     */
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

    /**
     * Updates all PENDING orders to PROCESSING status.
     * This method is called automatically by a scheduler.
     *
     * @return Number of orders updated
     */
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

    /**
     * Maps an Order entity to OrderResponse DTO.
     *
     * @param order The Order entity to map
     * @return OrderResponse containing the order details
     */
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

    /**
     * Maps an OrderItem entity to OrderItemDTO.
     *
     * @param orderItem The OrderItem entity to map
     * @return OrderItemDTO containing the item details
     */
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
