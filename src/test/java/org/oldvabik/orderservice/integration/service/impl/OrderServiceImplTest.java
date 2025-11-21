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
import org.springframework.data.domain.Page;
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
import java.util.ArrayList;
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

        testUser = new UserDto();
        testUser.setId(100L);
        testUser.setEmail("test@example.com");
        testUser.setName("John");
        testUser.setSurname("Doe");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));

        testItem = new Item();
        testItem.setName("Test Item");
        testItem.setPrice(BigDecimal.valueOf(99.99));
        testItem = itemRepository.save(testItem);

        when(accessChecker.canAccessUser(any(), any())).thenReturn(true);
    }

    @Test
    void createOrder_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);

        OrderCreateDto dto = new OrderCreateDto();
        dto.setEmail("test@example.com");

        OrderItemCreateDto itemDto = new OrderItemCreateDto();
        itemDto.setName("Test Item");
        itemDto.setQuantity(3);
        dto.setItems(List.of(itemDto));

        OrderDto result = orderService.createOrder(auth, dto);

        assertNotNull(result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreationDate());
        assertEquals(testUser, result.getUser());
        assertEquals(1, result.getItems().size());
        assertEquals("Test Item", result.getItems().get(0).getName());
        assertEquals(3, result.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(99.99), result.getItems().get(0).getPrice());
    }

    @Test
    void createOrder_accessDenied_throwsException() {
        UserDto otherUser = new UserDto();
        otherUser.setId(999L);
        otherUser.setEmail("other@example.com");

        when(userServiceClient.getUserByEmail(auth, "other@example.com")).thenReturn(otherUser);
        when(accessChecker.canAccessUser(any(), any())).thenReturn(false);

        OrderCreateDto dto = new OrderCreateDto();
        dto.setEmail("other@example.com");
        OrderItemCreateDto itemDto = new OrderItemCreateDto();
        itemDto.setName("Test Item");
        itemDto.setQuantity(1);
        dto.setItems(List.of(itemDto));

        assertThrows(AccessDeniedException.class, () -> orderService.createOrder(auth, dto));
        assertEquals(0, orderRepository.count());
    }

    @Test
    void createOrder_itemNotFound_throwsException() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);

        OrderCreateDto dto = new OrderCreateDto();
        dto.setEmail("test@example.com");
        OrderItemCreateDto itemDto = new OrderItemCreateDto();
        itemDto.setName("Unknown Item");
        itemDto.setQuantity(1);
        dto.setItems(List.of(itemDto));

        assertThrows(NotFoundException.class, () -> orderService.createOrder(auth, dto));
        assertEquals(0, orderRepository.count());
    }

    @Test
    void getOrderById_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderDto created = createTestOrder();
        OrderDto result = orderService.getOrderById(auth, created.getId());

        assertEquals(created.getId(), result.getId());
        assertEquals(testUser, result.getUser());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void getOrderById_accessDenied_throwsException() {
        UserDto otherUser = new UserDto();
        otherUser.setId(999L);
        otherUser.setEmail("other@example.com");

        Order order = new Order();
        order.setUserId(otherUser.getId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreationDate(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        Order savedOrder = orderRepository.save(order);

        final Long orderId = savedOrder.getId();

        when(userServiceClient.getUserById(auth, otherUser.getId())).thenReturn(otherUser);
        when(accessChecker.canAccessUser(any(), any())).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> orderService.getOrderById(auth, orderId));
    }

    @Test
    void getOrders_noFilters_returnsAllPaged() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        createTestOrder();
        createTestOrder();

        Page<OrderDto> result = orderService.getOrders(auth, 0, 10, null, null);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void getOrders_withIdsFilter_returnsOnlyMatching() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderDto order1 = createTestOrder();
        createTestOrder();

        Page<OrderDto> result = orderService.getOrders(auth, 0, 10, List.of(order1.getId()), null);

        assertEquals(1, result.getTotalElements());
        assertEquals(order1.getId(), result.getContent().get(0).getId());
    }

    @Test
    void getOrders_withStatusesFilter_returnsMatching() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        createTestOrder();

        OrderDto shippedOrder = createTestOrder();
        OrderUpdateDto updateDto = new OrderUpdateDto();
        updateDto.setStatus(OrderStatus.SHIPPED);
        orderService.updateOrder(auth, shippedOrder.getId(), updateDto);

        Page<OrderDto> result = orderService.getOrders(auth, 0, 10, null, List.of(OrderStatus.SHIPPED));

        assertEquals(1, result.getTotalElements());
        assertEquals(OrderStatus.SHIPPED, result.getContent().get(0).getStatus());
    }

    @Test
    void updateOrder_changeStatus_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderDto created = createTestOrder();

        OrderUpdateDto updateDto = new OrderUpdateDto();
        updateDto.setStatus(OrderStatus.DELIVERED);

        OrderDto result = orderService.updateOrder(auth, created.getId(), updateDto);

        assertEquals(OrderStatus.DELIVERED, result.getStatus());
    }

    @Test
    void deleteOrder_success() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);
        when(userServiceClient.getUserById(auth, testUser.getId())).thenReturn(testUser);

        OrderDto created = createTestOrder();
        assertTrue(orderRepository.existsById(created.getId()));

        orderService.deleteOrder(created.getId());
        assertFalse(orderRepository.existsById(created.getId()));
    }

    @Test
    void deleteOrder_notFound_throwsException() {
        assertThrows(NotFoundException.class, () -> orderService.deleteOrder(999L));
    }

    private OrderDto createTestOrder() {
        when(userServiceClient.getUserByEmail(auth, "test@example.com")).thenReturn(testUser);

        OrderCreateDto dto = new OrderCreateDto();
        dto.setEmail("test@example.com");

        OrderItemCreateDto itemDto = new OrderItemCreateDto();
        itemDto.setName("Test Item");
        itemDto.setQuantity(1);
        dto.setItems(List.of(itemDto));

        return orderService.createOrder(auth, dto);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }
}