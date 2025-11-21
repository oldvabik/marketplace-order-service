package org.oldvabik.orderservice.unit.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oldvabik.orderservice.client.UserServiceClient;
import org.oldvabik.orderservice.dto.*;
import org.oldvabik.orderservice.entity.*;
import org.oldvabik.orderservice.exception.NotFoundException;
import org.oldvabik.orderservice.mapper.OrderMapper;
import org.oldvabik.orderservice.repository.ItemRepository;
import org.oldvabik.orderservice.repository.OrderRepository;
import org.oldvabik.orderservice.security.AccessChecker;
import org.oldvabik.orderservice.service.impl.OrderServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private AccessChecker accessChecker;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final Long ORDER_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String EMAIL = "user@example.com";
    private static final String ITEM_NAME = "Laptop";

    @Test
    void createOrder_success() {
        var createDto = new OrderCreateDto();
        createDto.setEmail(EMAIL);
        createDto.setItems(List.of(itemCreateDto(ITEM_NAME, 2)));

        var user = userDto(USER_ID, EMAIL);
        var item = item(10L, ITEM_NAME, BigDecimal.valueOf(999.99));
        var savedOrder = order(ORDER_ID, USER_ID);
        var orderDtoFromMapper = new OrderDto(); // маппер возвращает DTO без user

        when(userServiceClient.getUserByEmail(authentication, EMAIL)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(true);
        when(itemRepository.findByName(ITEM_NAME)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toDto(savedOrder)).thenReturn(orderDtoFromMapper);

        OrderDto result = orderService.createOrder(authentication, createDto);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toDto(savedOrder);
    }

    @Test
    void createOrder_accessDenied_throwsException() {
        var dto = new OrderCreateDto();
        dto.setEmail(EMAIL);
        dto.setItems(List.of(itemCreateDto(ITEM_NAME, 1)));

        var user = userDto(USER_ID, EMAIL);

        when(userServiceClient.getUserByEmail(authentication, EMAIL)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> orderService.createOrder(authentication, dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_itemNotFound_throwsException() {
        var dto = new OrderCreateDto();
        dto.setEmail(EMAIL);
        dto.setItems(List.of(itemCreateDto("Unknown Item", 1)));

        var user = userDto(USER_ID, EMAIL);

        when(userServiceClient.getUserByEmail(authentication, EMAIL)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(true);
        when(itemRepository.findByName("Unknown Item")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.createOrder(authentication, dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_success() {
        var order = order(ORDER_ID, USER_ID);
        var user = userDto(USER_ID, EMAIL);
        var orderDtoFromMapper = new OrderDto();

        when(orderRepository.findByIdWithDetails(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderDtoFromMapper);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(true);

        OrderDto result = orderService.getOrderById(authentication, ORDER_ID);

        assertEquals(user, result.getUser());
    }

    @Test
    void getOrderById_notFound_throwsException() {
        when(orderRepository.findByIdWithDetails(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.getOrderById(authentication, ORDER_ID));
    }

    @Test
    void getOrderById_accessDenied_throwsException() {
        var order = order(ORDER_ID, USER_ID);
        var user = userDto(USER_ID, EMAIL);
        var orderDto = new OrderDto();

        when(orderRepository.findByIdWithDetails(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderDto);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> orderService.getOrderById(authentication, ORDER_ID));
    }

    @Test
    void getOrders_noFilters_returnsPagedOrders() {
        var order = order(ORDER_ID, USER_ID);
        var user = userDto(USER_ID, EMAIL);
        var orderDto = new OrderDto();
        var pageRequest = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(order), pageRequest, 1);

        when(orderRepository.findAllWithDetails(pageRequest)).thenReturn(page);
        when(orderMapper.toDto(order)).thenReturn(orderDto);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);

        Page<OrderDto> result = orderService.getOrders(authentication, 0, 10, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals(user, result.getContent().get(0).getUser());
    }

    @Test
    void getOrders_withIdsAndStatuses_usesCorrectRepositoryMethod() {
        var order = order(ORDER_ID, USER_ID);
        var user = userDto(USER_ID, EMAIL);
        var orderDto = new OrderDto();
        var pageRequest = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(order), pageRequest, 1);

        when(orderRepository.findByIdInAndStatusIn(
                eq(List.of(ORDER_ID)), eq(List.of(OrderStatus.PENDING)), eq(pageRequest)
        )).thenReturn(page);
        when(orderMapper.toDto(order)).thenReturn(orderDto);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);

        Page<OrderDto> result = orderService.getOrders(authentication, 0, 10, List.of(ORDER_ID), List.of(OrderStatus.PENDING));

        assertEquals(1, result.getContent().size());
        verify(orderRepository).findByIdInAndStatusIn(any(), any(), any());
    }

    @Test
    void updateOrder_success() {
        var order = order(ORDER_ID, USER_ID);
        var updateDto = new OrderUpdateDto();
        updateDto.setStatus(OrderStatus.SHIPPED);

        var user = userDto(USER_ID, EMAIL);

        var updatedDto = new OrderDto();
        updatedDto.setUserId(USER_ID);

        when(orderRepository.findByIdWithDetails(ORDER_ID)).thenReturn(Optional.of(order));
        doNothing().when(orderMapper).updateEntityFromDto(updateDto, order);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toDto(order)).thenReturn(updatedDto);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);

        OrderDto result = orderService.updateOrder(authentication, ORDER_ID, updateDto);

        assertEquals(user, result.getUser());
        verify(orderMapper).updateEntityFromDto(updateDto, order);
        verify(orderRepository).save(order);
    }

    @Test
    void deleteOrder_success() {
        var order = order(ORDER_ID, USER_ID);

        when(orderRepository.findByIdWithDetails(ORDER_ID)).thenReturn(Optional.of(order));

        orderService.deleteOrder(ORDER_ID);

        verify(orderRepository).delete(order);
    }

    @Test
    void deleteOrder_notFound_throwsException() {
        when(orderRepository.findByIdWithDetails(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.deleteOrder(ORDER_ID));
    }

    private OrderItemCreateDto itemCreateDto(String name, int quantity) {
        var dto = new OrderItemCreateDto();
        dto.setName(name);
        dto.setQuantity(quantity);
        return dto;
    }

    private Item item(Long id, String name, BigDecimal price) {
        var item = new Item();
        item.setId(id);
        item.setName(name);
        item.setPrice(price);
        return item;
    }

    private Order order(Long id, Long userId) {
        var order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setCreationDate(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        return order;
    }

    private UserDto userDto(Long id, String email) {
        var user = new UserDto();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test");
        user.setSurname("User");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        return user;
    }
}