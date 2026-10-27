package com.example.order.client;

import com.example.order.exception.InsufficientStockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * HTTP implementation of InventoryClient using Spring 6's RestClient.
 *
 * RestClient is the modern replacement for RestTemplate (non-reactive, fluent API).
 * The base URL comes from application.yml → overridden by INVENTORY_SERVICE_URL env var
 * in Docker Compose so service discovery is just DNS (container names as hostnames).
 */
@Slf4j
@Component
public class HttpInventoryClient implements InventoryClient {

    private final RestClient restClient;

    public HttpInventoryClient(
            RestClient.Builder builder,
            @Value("${inventory.service.url}") String inventoryUrl) {
        this.restClient = builder.baseUrl(inventoryUrl).build();
    }

    @Override
    public String reserveStock(String orderId, List<StockReservationItem> items) {
        log.info("Calling inventory-service to reserve stock for orderId={}", orderId);
        try {
            ReserveResponse response = restClient.post()
                    .uri("/api/v1/inventory/reserve")
                    .body(new ReserveRequest(orderId, items))
                    .retrieve()
                    .body(ReserveResponse.class);

            String reservationId = response != null ? response.reservationId() : null;
            log.info("Stock reserved: reservationId={}", reservationId);
            return reservationId;

        } catch (HttpClientErrorException ex) {
            // inventory-service returns 422 (UNPROCESSABLE_ENTITY) for insufficient stock
            if (ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw new InsufficientStockException("Insufficient stock for one or more items");
            }
            throw ex;
        }
    }

    @Override
    public void releaseStock(String reservationId) {
        log.info("Calling inventory-service to release reservationId={}", reservationId);
        restClient.post()
                .uri("/api/v1/inventory/release")
                .body(new ReleaseRequest(reservationId))
                .retrieve()
                .toBodilessEntity();
    }

    // ── Internal request/response DTOs (private to this adapter) ──────────

    record ReserveRequest(String orderId, List<StockReservationItem> items) {}

    record ReserveResponse(String reservationId) {}

    record ReleaseRequest(String reservationId) {}
}
