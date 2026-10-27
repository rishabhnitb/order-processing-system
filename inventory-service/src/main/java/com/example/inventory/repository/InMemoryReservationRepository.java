package com.example.inventory.repository;

import com.example.inventory.domain.Reservation;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Stores active reservations so we know what to release on compensation. */
@Repository
public class InMemoryReservationRepository {

    private final ConcurrentHashMap<String, Reservation> store = new ConcurrentHashMap<>();

    public Reservation save(Reservation reservation) {
        store.put(reservation.reservationId(), reservation);
        return reservation;
    }

    public Optional<Reservation> findById(String reservationId) {
        return Optional.ofNullable(store.get(reservationId));
    }

    public void delete(String reservationId) {
        store.remove(reservationId);
    }
}
