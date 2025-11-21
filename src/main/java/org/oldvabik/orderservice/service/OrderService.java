package org.oldvabik.orderservice.service;

import org.oldvabik.orderservice.dto.OrderCreateDto;
import org.oldvabik.orderservice.dto.OrderDto;
import org.oldvabik.orderservice.dto.OrderUpdateDto;
import org.oldvabik.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import java.util.List;

public interface OrderService {
    OrderDto createOrder(Authentication auth, OrderCreateDto dto);

    Page<OrderDto> getOrders(Authentication auth, Integer page, Integer size, List<Long> ids, List<OrderStatus> statuses);

    OrderDto getOrderById(Authentication auth, Long id);

    OrderDto updateOrder(Authentication auth, Long id, OrderUpdateDto dto);

    void deleteOrder(Long id);
}