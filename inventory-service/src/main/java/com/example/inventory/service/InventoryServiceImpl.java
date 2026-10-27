package com.example.inventory.service;

import com.example.inventory.domain.Reservation;
import com.example.inventory.domain.Reservation.ReservationItem;
import com.example.inventory.domain.StockItem;
import com.example.inventory.dto.ReserveStockRequest;
import com.example.inventory.dto.ReserveStockRequest.StockItemRequest;
import com.example.inventory.dto.ReserveStockResponse;
import com.example.inventory.dto.StockResponse;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.exception.ReservationNotFoundException;
import com.example.inventory.repository.InMemoryInventoryRepository;
import com.example.inventory.repository.InMemoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Stock reservation logic.
 *
 * reserveStock() is two-phased:
 *   Phase 1 — snapshot check: read each product and verify enough stock exists.
 *             Fast-fail before touching any data.
 *   Phase 2 — atomic per-item reservation via repository.reserve() (ConcurrentHashMap.compute).
 *             If any item fails (race condition: stock depleted between phases), compensate
 *             all already-reserved items from this request.
 *
 * This is a local (within inventory-service) mini-Saga — different from the
 * cross-service Saga in order-service, but the same compensation principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InMemoryInventoryRepository inventoryRepository;
    private final InMemoryReservationRepository reservationRepository;

    @Override
    public ReserveStockResponse reserveStock(ReserveStockRequest request) {
        // Phase 1: validate all items (snapshot — not guaranteed but catches obvious failures fast)
        for (StockItemRequest item : request.items()) {
            StockItem stock = inventoryRepository.findById(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));
            if (!stock.hasEnough(item.quantity())) {
                throw new InsufficientStockException(
                        item.productId(), item.quantity(), stock.availableQuantity());
            }
        }

        // Phase 2: atomic reservation with compensation on partial failure
        List<StockItemRequest> reserved = new ArrayList<>();
        try {
            for (StockItemRequest item : request.items()) {
                boolean success = inventoryRepository.reserve(item.productId(), item.quantity());
                if (!success) {
                    // Race condition: stock was depleted between phase 1 and phase 2
                    throw new InsufficientStockException(item.productId(), item.quantity(), 0);
                }
                reserved.add(item); // track what was reserved so we can roll back
            }
        } catch (Exception ex) {
            // Compensate: release anything we already reserved in this request
            log.warn("Partial reservation failed for orderId={}, compensating {} items",
                    request.orderId(), reserved.size());
            for (StockItemRequest item : reserved) {
                inventoryRepository.release(item.productId(), item.quantity());
            }
            throw ex;
        }

        // Save reservation record for later release
        List<ReservationItem> reservationItems = request.items().stream()
                .map(i -> new ReservationItem(i.productId(), i.quantity()))
                .toList();
        Reservation reservation = Reservation.create(request.orderId(), reservationItems);
        reservationRepository.save(reservation);

        log.info("Stock reserved: reservationId={} for orderId={}", reservation.reservationId(), request.orderId());
        return new ReserveStockResponse(reservation.reservationId());
    }

    @Override
    public void releaseStock(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        for (ReservationItem item : reservation.items()) {
            inventoryRepository.release(item.productId(), item.quantity());
        }

        reservationRepository.delete(reservationId);
        log.info("Stock released for reservationId={}", reservationId);
    }

    @Override
    public StockResponse getStock(String productId) {
        StockItem item = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return new StockResponse(
                item.productId(),
                item.productName(),
                item.availableQuantity(),
                item.reservedQuantity()
        );
    }
}
