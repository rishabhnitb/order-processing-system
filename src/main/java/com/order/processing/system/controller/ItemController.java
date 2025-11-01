package com.order.processing.system.controller;

import com.order.processing.system.dto.ItemRequest;
import com.order.processing.system.model.Item;
import com.order.processing.system.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Tag(name = "Item Management", description = "APIs for managing items")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    @Operation(summary = "Get all items")
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID")
    public ResponseEntity<Item> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItem(id));
    }

    @PostMapping
    @Operation(summary = "Create a new item")
    public ResponseEntity<Item> createItem(@Valid @RequestBody ItemRequest request) {
        Item item = new Item();
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        item.setDescription(request.getDescription());
        return new ResponseEntity<>(itemService.createItem(item), HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple items")
    public ResponseEntity<List<Item>> createItems(@Valid @RequestBody List<ItemRequest> requests) {
        List<Item> items = requests.stream()
                .map(req -> {
                    Item item = new Item();
                    item.setName(req.getName());
                    item.setPrice(req.getPrice());
                    item.setDescription(req.getDescription());
                    return item;
                })
                .toList();
        return new ResponseEntity<>(itemService.createItems(items), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an item")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Delete multiple items")
    public ResponseEntity<Void> deleteItems(@RequestBody List<Long> ids) {
        itemService.deleteItems(ids);
        return ResponseEntity.noContent().build();
    }
}
