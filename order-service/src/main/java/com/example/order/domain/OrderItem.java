package com.example.order.domain;

import java.math.BigDecimal;

/**
 * A single line item inside an Order.
 * Java record: immutable, constructor, equals/hashCode/toString generated automatically.
 */
public record OrderItem(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
) {}
