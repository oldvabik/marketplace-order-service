package org.oldvabik.orderservice.controller;

import jakarta.validation.Valid;
import org.oldvabik.orderservice.dto.ItemCreateDto;
import org.oldvabik.orderservice.dto.ItemDto;
import org.oldvabik.orderservice.dto.ItemUpdateDto;
import org.oldvabik.orderservice.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ItemDto> createItem(@Valid @RequestBody ItemCreateDto dto) {
        ItemDto createdItem = itemService.createItem(dto);
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ItemDto>> getItems(@RequestParam(defaultValue = "0") Integer page,
                                                  @RequestParam(defaultValue = "5") Integer size,
                                                  @RequestParam(required = false) String name) {
        Page<ItemDto> items = itemService.getItems(page, size, name);
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItemById(@PathVariable Long id) {
        ItemDto item = itemService.getItemById(id);
        return new ResponseEntity<>(item, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(@PathVariable Long id,
                                              @Valid @RequestBody ItemUpdateDto dto) {
        ItemDto updatedItem = itemService.updateItem(id, dto);
        return new ResponseEntity<>(updatedItem, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}