package com.order.processing.system.repository;

import com.order.processing.system.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Customer entity operations.
 * Extends JpaRepository to provide standard CRUD operations and custom query methods.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Finds a customer by their email address.
     *
     * @param email The email address to search for
     * @return Optional containing the customer if found
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Finds all active customers.
     *
     * @return List of active customers
     */
    List<Customer> findByActiveTrue();
}
