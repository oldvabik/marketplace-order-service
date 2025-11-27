package org.oldvabik.orderservice.controller;

import jakarta.validation.Valid;
import org.oldvabik.orderservice.dto.OrderCreateDto;
import org.oldvabik.orderservice.dto.OrderDto;
import org.oldvabik.orderservice.dto.OrderUpdateDto;
import org.oldvabik.orderservice.entity.OrderStatus;
import org.oldvabik.orderservice.service.OrderService;
import org.springframework.data.domain.Page;
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderDto>> getOrders(Authentication auth,
                                                    @RequestParam(defaultValue = "0") Integer page,
                                                    @RequestParam(defaultValue = "5") Integer size,
                                                    @RequestParam(required = false) List<Long> ids,
                                                    @RequestParam(required = false) List<OrderStatus> statuses) {
        Page<OrderDto> orders = orderService.getOrders(auth, page, size, ids, statuses);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(Authentication auth,
                                                 @PathVariable Long id) {
        OrderDto order = orderService.getOrderById(auth, id);
        return new ResponseEntity<>(order, HttpStatus.OK);
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
