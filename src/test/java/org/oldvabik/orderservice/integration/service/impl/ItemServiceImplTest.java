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
        itemRepository.deleteAll();
    }

    @Test
    void createItem_success() {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName("Laptop");
        dto.setPrice(BigDecimal.valueOf(1299.99));

        ItemDto result = itemService.createItem(dto);

        assertNotNull(result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals(0, BigDecimal.valueOf(1299.99).compareTo(result.getPrice()));
    }

    @Test
    void createItem_nameAlreadyExists_throwsException() {
        Item existing = new Item();
        existing.setName("Phone");
        existing.setPrice(BigDecimal.valueOf(899.99));
        itemRepository.save(existing);

        ItemCreateDto dto = new ItemCreateDto();
        dto.setName("Phone");
        dto.setPrice(BigDecimal.valueOf(999.99));

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> itemService.createItem(dto));

        assertTrue(ex.getMessage().contains("Phone"));
        assertEquals(1, itemRepository.count());
    }

    @Test
    void getItems_noFilter_returnsPaged() {
        saveItem("Item1", BigDecimal.TEN);
        saveItem("Item2", BigDecimal.valueOf(20));
        saveItem("Item3", BigDecimal.valueOf(30));

        Page<ItemDto> page = itemService.getItems(0, 2, null);

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void getItems_withNameFilter_returnsMatchingIgnoreCase() {
        saveItem("Gaming Laptop Pro", BigDecimal.valueOf(1999));
        saveItem("Laptop Basic", BigDecimal.valueOf(799));
        saveItem("Phone", BigDecimal.valueOf(999));

        Page<ItemDto> page = itemService.getItems(0, 10, "laptop");

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream()
                .allMatch(dto -> dto.getName().toLowerCase().contains("laptop")));
    }

    @Test
    void getItems_emptyNameFilter_returnsAll() {
        saveItem("A", BigDecimal.ONE);
        saveItem("B", BigDecimal.TEN);

        Page<ItemDto> page1 = itemService.getItems(0, 10, null);
        Page<ItemDto> page2 = itemService.getItems(0, 10, "");
        Page<ItemDto> page3 = itemService.getItems(0, 10, "   ");

        assertEquals(2, page1.getTotalElements());
        assertEquals(2, page2.getTotalElements());
        assertEquals(2, page3.getTotalElements());
    }

    @Test
    void getItemById_success() {
        Item item = new Item();
        item.setName("Tablet");
        item.setPrice(BigDecimal.valueOf(499.99));
        Item saved = itemRepository.save(item);

        ItemDto result = itemService.getItemById(saved.getId());

        assertEquals(saved.getId(), result.getId());
        assertEquals("Tablet", result.getName());
    }

    @Test
    void getItemById_notFound_throwsException() {
        assertThrows(NotFoundException.class, () -> itemService.getItemById(999L));
    }

    @Test
    void updateItem_onlyPrice_success() {
        Item item = new Item();
        item.setName("Keyboard");
        item.setPrice(BigDecimal.valueOf(49.99));
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setPrice(BigDecimal.valueOf(79.99));

        ItemDto updated = itemService.updateItem(saved.getId(), dto);

        assertEquals(0, BigDecimal.valueOf(79.99).compareTo(updated.getPrice()));
        assertEquals("Keyboard", updated.getName());
    }

    @Test
    void updateItem_changeName_toUnique_success() {
        Item item = new Item();
        item.setName("Mouse");
        item.setPrice(BigDecimal.valueOf(25.50));
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName("Gaming Mouse");

        ItemDto updated = itemService.updateItem(saved.getId(), dto);

        assertEquals("Gaming Mouse", updated.getName());
    }

    @Test
    void updateItem_changeName_toExisting_throwsException() {
        Item existing = new Item();
        existing.setName("ExistingName");
        existing.setPrice(BigDecimal.ONE);
        itemRepository.save(existing);

        Item item = new Item();
        item.setName("OldName");
        item.setPrice(BigDecimal.TEN);
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName("ExistingName");

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> itemService.updateItem(saved.getId(), dto));

        assertTrue(ex.getMessage().contains("ExistingName"));
        assertEquals("OldName", itemRepository.findById(saved.getId()).get().getName());
    }

    @Test
    void updateItem_sameName_noDuplicateCheck() {
        Item item = new Item();
        item.setName("SameName");
        item.setPrice(BigDecimal.valueOf(100));
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName("SameName");
        dto.setPrice(BigDecimal.valueOf(150));

        ItemDto updated = itemService.updateItem(saved.getId(), dto);

        assertEquals("SameName", updated.getName());
        assertEquals(0, BigDecimal.valueOf(150).compareTo(updated.getPrice()));
    }

    @Test
    void updateItem_nameIsNull_onlyPriceUpdates() {
        Item item = new Item();
        item.setName("Headphones");
        item.setPrice(BigDecimal.valueOf(89.99));
        Item saved = itemRepository.save(item);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setPrice(BigDecimal.valueOf(119.99));

        ItemDto updated = itemService.updateItem(saved.getId(), dto);

        assertEquals("Headphones", updated.getName());
        assertEquals(0, BigDecimal.valueOf(119.99).compareTo(updated.getPrice()));
    }

    @Test
    void updateItem_notFound_throwsException() {
        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setPrice(BigDecimal.ONE);

        assertThrows(NotFoundException.class, () -> itemService.updateItem(999L, dto));
    }

    @Test
    void deleteItem_success() {
        Item item = new Item();
        item.setName("Webcam");
        item.setPrice(BigDecimal.valueOf(69.99));
        Item saved = itemRepository.save(item);

        itemService.deleteItem(saved.getId());

        assertFalse(itemRepository.existsById(saved.getId()));
    }

    @Test
    void deleteItem_notFound_throwsException() {
        assertThrows(NotFoundException.class, () -> itemService.deleteItem(999L));
    }

    private void saveItem(String name, BigDecimal price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(price);
        itemRepository.save(item);
    }

    @AfterEach
    void tearDown() {
        itemRepository.deleteAll();
    }
}