package org.oldvabik.orderservice.service;

import org.oldvabik.orderservice.dto.OrderCreateDto;
import org.oldvabik.orderservice.dto.OrderDto;
import org.oldvabik.orderservice.dto.OrderUpdateDto;
import org.oldvabik.orderservice.entity.OrderStatus;
import org.springframework.security.core.Authentication;
import java.util.List;

public interface OrderService {
    OrderDto createOrder(Authentication auth, OrderCreateDto dto);

    OrderDto getOrderById(Authentication auth, Long id);

    List<OrderDto> getOrdersByIds(Authentication auth, List<Long> ids);

    List<OrderDto> getOrdersByStatuses(Authentication auth, List<OrderStatus> statuses);

    OrderDto updateOrder(Authentication auth, Long id, OrderUpdateDto dto);

    void deleteOrder(Long id);
}