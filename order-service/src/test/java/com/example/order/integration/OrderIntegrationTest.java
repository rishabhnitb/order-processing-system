package com.example.order.integration;

import com.example.order.client.InventoryClient;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderItemRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Full integration test: loads the complete Spring Boot context on a random port.
 *
 * InventoryClient is mocked with @MockBean — no real inventory-service needed.
 * All other beans (OrderServiceImpl, InMemoryOrderRepository, scheduler) are real.
 *
 * This test proves the full HTTP request → controller → service → repository
 * → response pipeline works end-to-end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private InventoryClient inventoryClient;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        when(inventoryClient.reserveStock(anyString(), anyList())).thenReturn("res-test-123");
        doNothing().when(inventoryClient).releaseStock(anyString());
    }

    @Test
    @DisplayName("Full lifecycle: create → get → list → cancel → try cancel again (409)")
    void fullOrderLifecycle() {
        // ── Create ───────────────────────────────────────────────────────
        CreateOrderRequest createReq = new CreateOrderRequest("cust-integration", List.of(
                new OrderItemRequest("prod-1", "Widget", 3, new BigDecimal("15.00"))
        ));

        ResponseEntity<OrderResponse> createResp =
                restTemplate.postForEntity("/api/v1/orders", createReq, OrderResponse.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getHeaders().getLocation()).isNotNull();

        OrderResponse created = createResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.customerId()).isEqualTo("cust-integration");
        assertThat(created.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(created.totalAmount()).isEqualByComparingTo("45.00");  // 3 × 15.00

        // ── Get by ID ────────────────────────────────────────────────────
        ResponseEntity<OrderResponse> getResp =
                restTemplate.getForEntity("/api/v1/orders/" + created.id(), OrderResponse.class);

        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody().status()).isEqualTo(OrderStatus.PENDING);

        // ── List all ─────────────────────────────────────────────────────
        ResponseEntity<OrderResponse[]> listResp =
                restTemplate.getForEntity("/api/v1/orders", OrderResponse[].class);

        assertThat(listResp.getBody()).hasSize(1);

        // ── List filtered by status ───────────────────────────────────────
        ResponseEntity<OrderResponse[]> filteredResp =
                restTemplate.getForEntity("/api/v1/orders?status=PENDING", OrderResponse[].class);

        assertThat(filteredResp.getBody()).hasSize(1);

        // ── Cancel ───────────────────────────────────────────────────────
        ResponseEntity<OrderResponse> cancelResp =
                restTemplate.postForEntity("/api/v1/orders/" + created.id() + "/cancel",
                        null, OrderResponse.class);

        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResp.getBody().status()).isEqualTo(OrderStatus.CANCELLED);

        // ── Try to cancel again — must fail with 409 ─────────────────────
        ResponseEntity<Object> doubleCancel =
                restTemplate.postForEntity("/api/v1/orders/" + created.id() + "/cancel",
                        null, Object.class);

        assertThat(doubleCancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("GET /orders/{id}: 404 for non-existent order")
    void getOrder_notFound_returns404() {
        ResponseEntity<Object> resp =
                restTemplate.getForEntity("/api/v1/orders/00000000-0000-0000-0000-000000000000",
                        Object.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
