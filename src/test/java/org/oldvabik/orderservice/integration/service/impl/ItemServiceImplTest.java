package org.oldvabik.orderservice.integration.service.impl;

import org.junit.jupiter.api.*;
import org.oldvabik.orderservice.dto.ItemCreateDto;
import org.oldvabik.orderservice.dto.ItemDto;
import org.oldvabik.orderservice.dto.ItemUpdateDto;
import org.oldvabik.orderservice.entity.Item;
import org.oldvabik.orderservice.exception.AlreadyExistsException;
import org.oldvabik.orderservice.exception.NotFoundException;
import org.oldvabik.orderservice.repository.ItemRepository;
import org.oldvabik.orderservice.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ItemServiceImplTest {

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
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll(); // чистим перед каждым тестом
    }

    @Test
    void createItem_success() {
        ItemCreateDto dto = ItemCreateDto.builder()
                .name("Laptop")
                .price(BigDecimal.valueOf(1299.99))
                .build();

        ItemDto result = itemService.createItem(dto);

        assertNotNull(result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals(0, BigDecimal.valueOf(1299.99).compareTo(result.getPrice()));
    }

    @Test
    void createItem_nameAlreadyExists_throwsAlreadyExistsException() {
        Item existing = Item.builder()
                .name("Phone")
                .price(BigDecimal.valueOf(899.99))
                .build();
        itemRepository.save(existing);

        ItemCreateDto dto = ItemCreateDto.builder()
                .name("Phone")
                .price(BigDecimal.valueOf(999.99))
                .build();

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> itemService.createItem(dto));

        assertTrue(ex.getMessage().contains("Phone"));
        assertEquals(1, itemRepository.count()); // ничего не добавилось
    }

    @Test
    void getItems_returnsPagedItems() {
        itemRepository.save(Item.builder().name("Item1").price(BigDecimal.TEN).build());
        itemRepository.save(Item.builder().name("Item2").price(BigDecimal.valueOf(20)).build());
        itemRepository.save(Item.builder().name("Item3").price(BigDecimal.valueOf(30)).build());

        Page<ItemDto> page = itemService.getItems(0, 2);

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals("Item1", page.getContent().get(0).getName());
    }

    @Test
    void getItemById_found() {
        Item item = Item.builder()
                .name("Tablet")
                .price(BigDecimal.valueOf(499.99))
                .build();
        Item saved = itemRepository.save(item);

        ItemDto result = itemService.getItemById(saved.getId());

        assertEquals(saved.getId(), result.getId());
        assertEquals("Tablet", result.getName());
    }

    @Test
    void getItemById_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> itemService.getItemById(999L));
    }

    @Test
    void getItemByName_found() {
        itemRepository.save(Item.builder().name("Monitor").price(BigDecimal.valueOf(299.99)).build());

        ItemDto result = itemService.getItemByName("Monitor");

        assertEquals("Monitor", result.getName());
    }

    @Test
    void getItemByName_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> itemService.getItemByName("GhostItem"));
    }

    @Test
    void updateItem_onlyPrice_success() {
        Item item = Item.builder().name("Keyboard").price(BigDecimal.valueOf(49.99)).build();
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = ItemUpdateDto.builder()
                .price(BigDecimal.valueOf(79.99))
                .build();

        ItemDto updated = itemService.updateItem(saved.getId(), dto);

        assertEquals(0, BigDecimal.valueOf(79.99).compareTo(updated.getPrice()));
        assertEquals("Keyboard", updated.getName());
    }

    @Test
    void updateItem_changeName_toUnique_success() {
        Item item = Item.builder().name("Mouse").price(BigDecimal.valueOf(25.50)).build();
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = ItemUpdateDto.builder()
                .name("Gaming Mouse")
                .build();

        ItemDto updated = itemService.updateItem(saved.getId(), dto);

        assertEquals("Gaming Mouse", updated.getName());
    }

    @Test
    void updateItem_changeName_toExisting_throwsAlreadyExistsException() {
        itemRepository.save(Item.builder().name("ExistingName").price(BigDecimal.ONE).build());

        Item item = Item.builder().name("OldName").price(BigDecimal.TEN).build();
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = ItemUpdateDto.builder()
                .name("ExistingName")
                .build();

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> itemService.updateItem(saved.getId(), dto));

        assertTrue(ex.getMessage().contains("ExistingName"));
        // имя не поменялось
        assertEquals("OldName", itemRepository.findById(saved.getId()).get().getName());
    }

    @Test
    void updateItem_nameNotChanged_noDuplicateCheck() {
        Item item = Item.builder().name("SameName").price(BigDecimal.valueOf(100)).build();
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = ItemUpdateDto.builder()
                .name("SameName")  // то же самое имя
                .price(BigDecimal.valueOf(150))
                .build();

        ItemDto updated = itemService.updateItem(saved.getId(), dto);

        assertEquals("SameName", updated.getName());
        assertEquals(0, BigDecimal.valueOf(150).compareTo(updated.getPrice()));
        // проверка: не должно быть ошибки, даже если имя существует (это он сам)
    }

    @Test
    void updateItem_nameIsNull_onlyPriceUpdates() {
        Item item = Item.builder().name("Headphones").price(BigDecimal.valueOf(89.99)).build();
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = ItemUpdateDto.builder()
                .name(null)
                .price(BigDecimal.valueOf(119.99))
                .build();

        ItemDto updated = itemService.updateItem(saved.getId(), dto);

        assertEquals("Headphones", updated.getName());
        assertEquals(0, BigDecimal.valueOf(119.99).compareTo(updated.getPrice()));
    }

    @Test
    void updateItem_notFound_throwsNotFoundException() {
        ItemUpdateDto dto = ItemUpdateDto.builder().price(BigDecimal.ONE).build();
        assertThrows(NotFoundException.class, () -> itemService.updateItem(999L, dto));
    }

    @Test
    void deleteItem_success() {
        Item item = Item.builder().name("Webcam").price(BigDecimal.valueOf(69.99)).build();
        Item saved = itemRepository.save(item);

        itemService.deleteItem(saved.getId());

        assertFalse(itemRepository.existsById(saved.getId()));
    }

    @Test
    void deleteItem_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> itemService.deleteItem(999L));
    }
}