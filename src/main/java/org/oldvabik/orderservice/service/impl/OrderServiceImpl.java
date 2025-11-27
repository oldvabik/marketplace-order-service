package org.oldvabik.orderservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.oldvabik.orderservice.client.UserServiceClient;
import org.oldvabik.orderservice.dto.*;
import org.oldvabik.orderservice.entity.Item;
import org.oldvabik.orderservice.entity.Order;
import org.oldvabik.orderservice.entity.OrderItem;
import org.oldvabik.orderservice.entity.OrderStatus;
import org.oldvabik.orderservice.exception.NotFoundException;
import org.oldvabik.orderservice.mapper.OrderMapper;
import org.oldvabik.orderservice.repository.ItemRepository;
import org.oldvabik.orderservice.repository.OrderRepository;
import org.oldvabik.orderservice.security.AccessChecker;
import org.oldvabik.orderservice.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;
    private final AccessChecker accessChecker;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ItemRepository itemRepository,
                            OrderMapper orderMapper,
                            UserServiceClient userServiceClient,
                            AccessChecker accessChecker) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.orderMapper = orderMapper;
        this.userServiceClient = userServiceClient;
        this.accessChecker = accessChecker;
    }

    @Override
    @Transactional
    public OrderDto createOrder(Authentication auth, OrderCreateDto dto) {
        log.debug("[OrderService] createOrder: email={}", dto.getEmail());

        UserDto user = userServiceClient.getUserByEmail(auth, dto.getEmail());

        if (!accessChecker.canAccessUser(auth, user)) {
            log.warn("[OrderService] createOrder: access denied for email={}", auth.getName());
            throw new AccessDeniedException("Access denied");
        }

        Order order = Order.builder()
                .userId(user.getId())
                .status(OrderStatus.PENDING)
                .creationDate(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemCreateDto itemDto : dto.getItems()) {
            Item item = itemRepository.findByName(itemDto.getName())
                    .orElseThrow(() -> new NotFoundException(
                            "Item not found: " + itemDto.getName())
                    );

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setItem(item);
            oi.setQuantity(itemDto.getQuantity());
            orderItems.add(oi);
        }

        order.setItems(orderItems);

        Order saved = orderRepository.save(order);

        OrderDto response = orderMapper.toDto(saved);
        response.setUser(user);

        log.info("[OrderService] createOrder: created id={}", saved.getId());
        return response;
    }

    @Override
    public Page<OrderDto> getOrders(Authentication auth,
                                    Integer page,
                                    Integer size,
                                    List<Long> ids,
                                    List<OrderStatus> statuses) {
        log.debug("[OrderService] getOrders: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders;

        boolean hasIds = ids != null && !ids.isEmpty();
        boolean hasStatuses = statuses != null && !statuses.isEmpty();

        if (!hasIds && !hasStatuses) {
            orders = orderRepository.findAllWithDetails(pageable);
        } else if (hasIds && hasStatuses) {
            orders = orderRepository.findByIdInAndStatusIn(ids, statuses, pageable);
        } else if (hasIds) {
            orders = orderRepository.findByIdIn(ids, pageable);
        } else {
            orders = orderRepository.findByStatusIn(statuses, pageable);
        }

        Page<OrderDto> result = orders.map(order -> {
            OrderDto dto = orderMapper.toDto(order);
            dto.setUser(userServiceClient.getUserById(auth, order.getUserId()));
            return dto;
        });

        log.info("[OrderService] getOrders: returning {} orders", result.getContent().size());
        return result;
    }

    @Override
    public OrderDto getOrderById(Authentication auth, Long id) {
        log.debug("[OrderService] getOrderById: id={}", id);

        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> {
                    log.warn("[OrderService] getOrderById: id={} not found", id);
                    return new NotFoundException("order with id " + id + " not found");
                });

        OrderDto dto = orderMapper.toDto(order);
        dto.setUser(userServiceClient.getUserById(auth, order.getUserId()));

        if (!accessChecker.canAccessUser(auth, dto.getUser())) {
            log.warn("[OrderService] getOrderById: access denied for email={}", auth.getName());
            throw new AccessDeniedException("Access denied");
        }

        log.info("[OrderService] getOrderById: found id={}", id);
        return dto;
    }

    @Override
    @Transactional
    public OrderDto updateOrder(Authentication auth, Long id, OrderUpdateDto dto) {
        log.debug("[OrderService] updateOrder: id={}", id);

        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> {
                    log.warn("[OrderService] updateOrder: id={} not found", id);
                    return new NotFoundException("order with id " + id + " not found");
                });

        orderMapper.updateEntityFromDto(dto, order);
        Order updatedOrder = orderRepository.save(order);
        OrderDto updatedOrderDto = orderMapper.toDto(updatedOrder);
        updatedOrderDto.setUser(userServiceClient.getUserById(auth, updatedOrderDto.getUserId()));

        log.info("[OrderService] updateOrder: updated id={}", id);
        return updatedOrderDto;
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        log.debug("[OrderService] deleteOrder: id={}", id);

        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> {
                    log.warn("[OrderService] deleteOrder: id={} not found", id);
                    return new NotFoundException("order with id " + id + " not found");
                });

        orderRepository.delete(order);

        log.info("[OrderService] deleteOrder: deleted id={}", id);
    }
}
