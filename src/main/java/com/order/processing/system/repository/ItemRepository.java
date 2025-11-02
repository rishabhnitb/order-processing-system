package com.order.processing.system.repository;

import com.order.processing.system.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository interface for Item entity operations.
 * Extends JpaRepository to provide standard CRUD operations and custom query methods.
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Finds items with price less than or equal to the specified amount.
     *
     * @param price The maximum price to search for
     * @return List of items within the price range
     */
    List<Item> findByPriceLessThanEqual(Double price);

    /**
     * Finds items containing the specified name (case-insensitive).
     *
     * @param name The name to search for
     * @return List of items matching the name pattern
     */
    List<Item> findByNameContainingIgnoreCase(String name);

    /**
     * Finds items by their IDs, ordered by name.
     *
     * @param ids Collection of item IDs to search for
     * @return List of items matching the provided IDs
     */
    List<Item> findByIdInOrderByNameAsc(Collection<Long> ids);
}
