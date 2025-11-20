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
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setEmail(EMAIL);
        createDto.setItems(List.of(itemCreateDto(ITEM_NAME, 2)));

        UserDto user = userDto(USER_ID, EMAIL);
        Item item = item(10L, ITEM_NAME, BigDecimal.valueOf(999.99));
        Order orderEntity = order(ORDER_ID, USER_ID);

        // ВАЖНО: маппер должен установить userId!
        OrderDto mappedDto = new OrderDto();
        mappedDto.setUserId(USER_ID); // <-- Это критично!

        when(userServiceClient.getUserByEmail(authentication, EMAIL)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(true);
        when(itemRepository.findByName(ITEM_NAME)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(orderEntity);
        when(orderMapper.toDto(orderEntity)).thenReturn(mappedDto);

        OrderDto result = orderService.createOrder(authentication, createDto);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_accessDenied_throwsException() {
        OrderCreateDto dto = new OrderCreateDto();
        dto.setEmail(EMAIL);
        dto.setItems(List.of(itemCreateDto(ITEM_NAME, 1)));

        UserDto user = userDto(USER_ID, EMAIL);

        when(userServiceClient.getUserByEmail(authentication, EMAIL)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> orderService.createOrder(authentication, dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_itemNotFound_throwsException() {
        OrderCreateDto dto = new OrderCreateDto();
        dto.setEmail(EMAIL);
        dto.setItems(List.of(itemCreateDto("Unknown", 1)));

        UserDto user = userDto(USER_ID, EMAIL);

        when(userServiceClient.getUserByEmail(authentication, EMAIL)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(true);
        when(itemRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.createOrder(authentication, dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_success() {
        Order orderEntity = order(ORDER_ID, USER_ID);
        UserDto user = userDto(USER_ID, EMAIL);

        OrderDto dtoFromMapper = new OrderDto();
        dtoFromMapper.setUserId(USER_ID); // <-- обязательно!

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderEntity));
        when(orderMapper.toDto(orderEntity)).thenReturn(dtoFromMapper);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(true);

        OrderDto result = orderService.getOrderById(authentication, ORDER_ID);

        assertEquals(user, result.getUser());
    }

    @Test
    void getOrderById_notFound_throwsException() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.getOrderById(authentication, ORDER_ID));
    }

    @Test
    void getOrderById_accessDenied_throwsException() {
        Order orderEntity = order(ORDER_ID, USER_ID);
        UserDto user = userDto(USER_ID, EMAIL);
        OrderDto dto = new OrderDto();
        dto.setUserId(USER_ID);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderEntity));
        when(orderMapper.toDto(orderEntity)).thenReturn(dto);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);
        when(accessChecker.canAccessUser(authentication, user)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> orderService.getOrderById(authentication, ORDER_ID));
    }

    @Test
    void getOrdersByIds_returnsList() {
        Order orderEntity = order(ORDER_ID, USER_ID);
        UserDto user = userDto(USER_ID, EMAIL);

        OrderDto dto = new OrderDto();
        dto.setUserId(USER_ID); // <-- критично!

        when(orderRepository.findByIdIn(List.of(ORDER_ID))).thenReturn(List.of(orderEntity));
        when(orderMapper.toDto(orderEntity)).thenReturn(dto);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);

        List<OrderDto> result = orderService.getOrdersByIds(authentication, List.of(ORDER_ID));

        assertEquals(1, result.size());
        assertEquals(user, result.get(0).getUser());
    }

    @Test
    void getOrdersByStatuses_returnsList() {
        Order orderEntity = order(ORDER_ID, USER_ID);
        UserDto user = userDto(USER_ID, EMAIL);

        OrderDto dto = new OrderDto();
        dto.setUserId(USER_ID); // <-- обязательно!

        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(orderEntity));
        when(orderMapper.toDto(orderEntity)).thenReturn(dto);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);

        List<OrderDto> result = orderService.getOrdersByStatuses(authentication, List.of(OrderStatus.PENDING));

        assertEquals(1, result.size());
        assertEquals(user, result.get(0).getUser());
    }

    @Test
    void updateOrder_success() {
        Order orderEntity = order(ORDER_ID, USER_ID);
        OrderUpdateDto updateDto = new OrderUpdateDto();
        updateDto.setStatus(OrderStatus.SHIPPED);
        UserDto user = userDto(USER_ID, EMAIL);

        OrderDto updatedDto = new OrderDto();
        updatedDto.setUserId(USER_ID); // <-- обязательно!

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderEntity));
        doNothing().when(orderMapper).updateEntityFromDto(updateDto, orderEntity);
        when(orderRepository.save(orderEntity)).thenReturn(orderEntity);
        when(orderMapper.toDto(orderEntity)).thenReturn(updatedDto);
        when(userServiceClient.getUserById(authentication, USER_ID)).thenReturn(user);

        OrderDto result = orderService.updateOrder(authentication, ORDER_ID, updateDto);

        assertEquals(user, result.getUser());
        verify(orderMapper).updateEntityFromDto(updateDto, orderEntity);
    }

    @Test
    void deleteOrder_success() {
        Order orderEntity = order(ORDER_ID, USER_ID);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderEntity));

        orderService.deleteOrder(ORDER_ID);

        verify(orderRepository).delete(orderEntity);
    }

    @Test
    void deleteOrder_notFound_throwsException() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.deleteOrder(ORDER_ID));
    }

    // Вспомогательные методы
    private OrderItemCreateDto itemCreateDto(String name, int qty) {
        OrderItemCreateDto dto = new OrderItemCreateDto();
        dto.setName(name);
        dto.setQuantity(qty);
        return dto;
    }

    private Item item(Long id, String name, BigDecimal price) {
        Item i = new Item();
        i.setId(id);
        i.setName(name);
        i.setPrice(price);
        return i;
    }

    private Order order(Long id, Long userId) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setStatus(OrderStatus.PENDING);
        o.setCreationDate(LocalDateTime.now());
        o.setItems(new ArrayList<>());
        return o;
    }

    private UserDto userDto(Long id, String email) {
        UserDto u = new UserDto();
        u.setId(id);
        u.setEmail(email);
        u.setName("Test");
        u.setSurname("User");
        u.setBirthDate(LocalDate.of(1990, 1, 1));
        return u;
    }
}