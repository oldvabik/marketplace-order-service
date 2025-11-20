package org.oldvabik.orderservice.controller;

import jakarta.validation.Valid;
import org.oldvabik.orderservice.dto.OrderCreateDto;
import org.oldvabik.orderservice.dto.OrderDto;
import org.oldvabik.orderservice.dto.OrderUpdateDto;
import org.oldvabik.orderservice.entity.OrderStatus;
import org.oldvabik.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(Authentication auth,
                                                @Valid @RequestBody OrderCreateDto dto) {
        OrderDto createdOrder = orderService.createOrder(auth, dto);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(Authentication auth,
                                                 @PathVariable Long id) {
        OrderDto order = orderService.getOrderById(auth, id);
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-ids")
    public ResponseEntity<List<OrderDto>> getOrdersByIds(Authentication auth,
                                                         @RequestParam List<Long> ids) {
        List<OrderDto> orders = orderService.getOrdersByIds(auth, ids);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-statuses")
    public ResponseEntity<List<OrderDto>> getOrdersByStatuses(Authentication auth,
                                                              @RequestParam List<OrderStatus> statuses) {
        List<OrderDto> orders = orderService.getOrdersByStatuses(auth, statuses);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(Authentication auth,
                                                @PathVariable Long id,
                                                @Valid @RequestBody OrderUpdateDto dto) {
        OrderDto updatedOrder = orderService.updateOrder(auth, id, dto);
        return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
