package com.order.processing.system.dto;

import com.order.processing.system.model.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderResponse {
    private UUID id;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CustomerDTO customer;
    private List<OrderItemDTO> items;
    private double totalAmount;

    @Data
    public static class OrderItemDTO {
        private Long itemId;
        private String itemName;
        private Double itemPrice;
        private Integer quantity;
        private Double subtotal;
    }

    @Data
    public static class CustomerDTO {
        private Long id;
        private String name;
        private String email;
    }
}
