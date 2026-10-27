package com.example.order.mapper;

import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.dto.OrderItemRequest;
import com.example.order.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts between domain objects and DTOs.
 *
 * Keeping mapping logic here (not in controller or service) follows Single Responsibility:
 *   - Controller: HTTP concerns only
 *   - Service: business logic only
 *   - Mapper: shape transformation only
 */
@Component
public class OrderMapper {

    public OrderItem toOrderItem(OrderItemRequest request) {
        return new OrderItem(
                request.productId(),
                request.productName(),
                request.quantity(),
                request.unitPrice()
        );
    }

    public List<OrderItem> toOrderItems(List<OrderItemRequest> requests) {
        return requests.stream().map(this::toOrderItem).toList();
    }

    public OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.items().stream()
                .map(item -> new OrderResponse.OrderItemResponse(
                        item.productId(),
                        item.productName(),
                        item.quantity(),
                        item.unitPrice()))
                .toList();

        return new OrderResponse(
                order.id(),
                order.customerId(),
                itemResponses,
                order.status(),
                order.totalAmount(),
                order.createdAt(),
                order.updatedAt()
        );
    }
}
