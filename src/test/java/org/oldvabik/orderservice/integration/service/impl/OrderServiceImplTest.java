package org.oldvabik.orderservice.integration.service.impl;

import org.junit.jupiter.api.*;
import org.oldvabik.orderservice.client.UserServiceClient;
import org.oldvabik.orderservice.dto.*;
import org.oldvabik.orderservice.entity.Item;
import org.oldvabik.orderservice.entity.Order;
import org.oldvabik.orderservice.entity.OrderStatus;
import org.oldvabik.orderservice.exception.NotFoundException;
import org.oldvabik.orderservice.repository.ItemRepository;
import org.oldvabik.orderservice.repository.OrderRepository;
import org.oldvabik.orderservice.security.AccessChecker;
import org.oldvabik.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceImplTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("marketplace")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ItemRepository itemRepository;

    @MockBean
    private UserServiceClient userServiceClient;
    @MockBean
    private AccessChecker accessChecker;

    private Authentication auth;
    private UserDto testUser;
    private Item testItem;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();

        auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");

        testUser = UserDto.builder()
                .id(100L)
                .email("test@example.com")
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        testItem = itemRepository.save(Item.builder()
                .name("Test Item")
                .price(BigDecimal.valueOf(99.99))
                .build());

        when(accessChecker.canAccessUser(any(), any())).thenReturn(true);
    }

    @Test
    void createOrder_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);

        OrderCreateDto dto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder()
                        .name("Test Item")
                        .quantity(2)
                        .build()))
                .build();

        OrderDto result = orderService.createOrder(auth, dto);

        assertNotNull(result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreationDate());
        assertEquals(testUser, result.getUser());
        assertEquals(1, result.getItems().size());
        assertEquals("Test Item", result.getItems().get(0).getName());
        assertEquals(2, result.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(99.99), result.getItems().get(0).getPrice());
    }

    @Test
    void createOrder_withMultipleItems_success() {
        Item secondItem = itemRepository.save(Item.builder()
                .name("Second Item")
                .price(BigDecimal.valueOf(49.99))
                .build());

        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);

        OrderCreateDto dto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(
                        OrderItemCreateDto.builder().name("Test Item").quantity(1).build(),
                        OrderItemCreateDto.builder().name("Second Item").quantity(3).build()
                ))
                .build();

        OrderDto result = orderService.createOrder(auth, dto);

        assertEquals(2, result.getItems().size());
        assertEquals(BigDecimal.valueOf(99.99), result.getItems().get(0).getPrice());
        assertEquals(BigDecimal.valueOf(49.99), result.getItems().get(1).getPrice());
    }

    @Test
    void createOrder_accessDenied_throwsAccessDeniedException() {
        UserDto otherUser = UserDto.builder().id(999L).email("other@example.com").build();
        when(userServiceClient.getUserByEmail(auth, "other@example.com")).thenReturn(otherUser);
        when(accessChecker.canAccessUser(any(), any())).thenReturn(false);

        OrderCreateDto dto = OrderCreateDto.builder()
                .email("other@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        assertThrows(AccessDeniedException.class, () -> orderService.createOrder(auth, dto));
        assertEquals(0, orderRepository.count());
    }

    @Test
    void createOrder_itemNotFound_throwsNotFoundException() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);

        OrderCreateDto dto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("NonExistentItem").quantity(1).build()))
                .build();

        assertThrows(NotFoundException.class, () -> orderService.createOrder(auth, dto));
        assertEquals(0, orderRepository.count());
    }

    @Test
    void getOrderById_success() {
        // Создаем заказ через сервис
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderCreateDto createDto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        OrderDto createdOrder = orderService.createOrder(auth, createDto);
        OrderDto result = orderService.getOrderById(auth, createdOrder.getId());

        assertEquals(createdOrder.getId(), result.getId());
        assertEquals(testUser, result.getUser());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void getOrderById_notFound_throwsNotFoundException() {
        // Ничего не мокаем для userServiceClient.getUserById — он не должен вызваться

        assertThrows(NotFoundException.class, () -> orderService.getOrderById(auth, 999L));

        // опционально: убедимся, что клиент не был вызван
        verifyNoInteractions(userServiceClient);
    }

    @Test
    void getOrderById_accessDenied_throwsAccessDeniedException() {
        // Создаем заказ для другого пользователя
        UserDto otherUser = UserDto.builder().id(999L).email("other@example.com").build();

        Order order = Order.builder()
                .userId(otherUser.getId())
                .status(OrderStatus.PENDING)
                .creationDate(LocalDateTime.now())
                .build();
        Order saved = orderRepository.save(order);

        when(userServiceClient.getUserById(auth, otherUser.getId())).thenReturn(otherUser);
        when(accessChecker.canAccessUser(any(), any())).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> orderService.getOrderById(auth, saved.getId()));
    }

    @Test
    void getOrdersByIds_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        // Создаем несколько заказов
        OrderCreateDto createDto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        OrderDto order1 = orderService.createOrder(auth, createDto);
        OrderDto order2 = orderService.createOrder(auth, createDto);

        List<OrderDto> result = orderService.getOrdersByIds(auth, List.of(order1.getId(), order2.getId()));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(order1.getId())));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(order2.getId())));
    }

    @Test
    void getOrdersByIds_emptyList_returnsEmptyList() {
        List<OrderDto> result = orderService.getOrdersByIds(auth, List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getOrdersByIds_someNotFound_returnsFoundOrders() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderCreateDto createDto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        OrderDto existingOrder = orderService.createOrder(auth, createDto);

        List<OrderDto> result = orderService.getOrdersByIds(auth,
                List.of(existingOrder.getId(), 999L, 1000L));

        assertEquals(1, result.size());
        assertEquals(existingOrder.getId(), result.get(0).getId());
    }

    @Test
    void getOrdersByStatuses_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        // Создаем заказы с разными статусами
        OrderCreateDto createDto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        orderService.createOrder(auth, createDto); // PENDING по умолчанию

        List<OrderDto> result = orderService.getOrdersByStatuses(auth, List.of(OrderStatus.PENDING));

        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(o -> o.getStatus() == OrderStatus.PENDING));
    }

    @Test
    void getOrdersByStatuses_multipleStatuses_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderCreateDto createDto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        OrderDto pendingOrder = orderService.createOrder(auth, createDto);

        // Обновляем статус одного заказа
        OrderUpdateDto updateDto = OrderUpdateDto.builder().status(OrderStatus.SHIPPED).build();
        orderService.updateOrder(auth, pendingOrder.getId(), updateDto);

        // Создаем еще один PENDING заказ
        OrderDto newPendingOrder = orderService.createOrder(auth, createDto);

        List<OrderDto> result = orderService.getOrdersByStatuses(auth,
                List.of(OrderStatus.PENDING, OrderStatus.SHIPPED));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getStatus() == OrderStatus.PENDING));
        assertTrue(result.stream().anyMatch(o -> o.getStatus() == OrderStatus.SHIPPED));
    }

    @Test
    void getOrdersByStatuses_noMatches_returnsEmptyList() {
        List<OrderDto> result = orderService.getOrdersByStatuses(auth,
                List.of(OrderStatus.CANCELLED, OrderStatus.DELIVERED));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateOrder_changeStatus_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderCreateDto createDto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        OrderDto createdOrder = orderService.createOrder(auth, createDto);

        OrderUpdateDto updateDto = OrderUpdateDto.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        OrderDto result = orderService.updateOrder(auth, createdOrder.getId(), updateDto);

        assertEquals(OrderStatus.SHIPPED, result.getStatus());
        assertEquals(createdOrder.getId(), result.getId());
        assertEquals(createdOrder.getUser(), result.getUser());
        assertEquals(createdOrder.getItems().size(), result.getItems().size());
    }

    @Test
    void updateOrder_notFound_throwsNotFoundException() {
        OrderUpdateDto dto = OrderUpdateDto.builder().status(OrderStatus.CANCELLED).build();
        assertThrows(NotFoundException.class, () -> orderService.updateOrder(auth, 999L, dto));
    }

    @Test
    void updateOrder_multipleFields_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderCreateDto createDto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        OrderDto createdOrder = orderService.createOrder(auth, createDto);

        // В реальности OrderUpdateDto может иметь больше полей
        OrderUpdateDto updateDto = OrderUpdateDto.builder()
                .status(OrderStatus.DELIVERED)
                .build();

        OrderDto result = orderService.updateOrder(auth, createdOrder.getId(), updateDto);

        assertEquals(OrderStatus.DELIVERED, result.getStatus());
    }

    @Test
    void deleteOrder_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderCreateDto createDto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        OrderDto createdOrder = orderService.createOrder(auth, createDto);

        assertTrue(orderRepository.existsById(createdOrder.getId()));

        orderService.deleteOrder(createdOrder.getId());

        assertFalse(orderRepository.existsById(createdOrder.getId()));
    }

    @Test
    void deleteOrder_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> orderService.deleteOrder(999L));
    }

    @Test
    void deleteOrder_alreadyDeleted_throwsNotFoundException() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderCreateDto createDto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder().name("Test Item").quantity(1).build()))
                .build();

        OrderDto createdOrder = orderService.createOrder(auth, createDto);

        orderService.deleteOrder(createdOrder.getId());

        assertThrows(NotFoundException.class, () -> orderService.deleteOrder(createdOrder.getId()));
    }

    // Тесты на граничные случаи
    @Test
    void createOrder_withMaxQuantity_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);

        OrderCreateDto dto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder()
                        .name("Test Item")
                        .quantity(Integer.MAX_VALUE)
                        .build()))
                .build();

        OrderDto result = orderService.createOrder(auth, dto);

        assertNotNull(result.getId());
        assertEquals(Integer.MAX_VALUE, result.getItems().get(0).getQuantity());
    }

    @Test
    void createOrder_withMinQuantity_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);

        OrderCreateDto dto = OrderCreateDto.builder()
                .email("test@example.com")
                .items(List.of(OrderItemCreateDto.builder()
                        .name("Test Item")
                        .quantity(1)
                        .build()))
                .build();

        OrderDto result = orderService.createOrder(auth, dto);

        assertNotNull(result.getId());
        assertEquals(1, result.getItems().get(0).getQuantity());
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }
}