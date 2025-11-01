package com.order.processing.system.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long itemId;
    private Integer quantity;
}
