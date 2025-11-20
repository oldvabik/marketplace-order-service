package org.oldvabik.orderservice.unit.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oldvabik.orderservice.dto.ItemCreateDto;
import org.oldvabik.orderservice.dto.ItemDto;
import org.oldvabik.orderservice.dto.ItemUpdateDto;
import org.oldvabik.orderservice.entity.Item;
import org.oldvabik.orderservice.exception.AlreadyExistsException;
import org.oldvabik.orderservice.exception.NotFoundException;
import org.oldvabik.orderservice.mapper.ItemMapper;
import org.oldvabik.orderservice.repository.ItemRepository;
import org.oldvabik.orderservice.service.impl.ItemServiceImpl;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemServiceImpl itemService;

    private static final Long ID = 1L;
    private static final String NAME = "Laptop";
    private static final String NEW_NAME = "Gaming PC";
    private static final BigDecimal PRICE = BigDecimal.valueOf(1299.99);

    @Test
    void createItem_success() {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName(NAME);
        dto.setPrice(PRICE);

        Item entity = new Item();
        Item saved = new Item();
        saved.setId(ID);
        saved.setName(NAME);
        saved.setPrice(PRICE);

        when(itemRepository.findByName(NAME)).thenReturn(Optional.empty());
        when(itemMapper.toEntity(dto)).thenReturn(entity);
        when(itemRepository.save(entity)).thenReturn(saved);
        when(itemMapper.toDto(saved)).thenReturn(new ItemDto(ID, NAME, PRICE));

        ItemDto result = itemService.createItem(dto);

        assertNotNull(result);
        assertEquals(ID, result.getId());
        assertEquals(NAME, result.getName());
        verify(itemRepository).save(entity);
    }

    @Test
    void createItem_nameAlreadyExists_throwsException() {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName(NAME);
        dto.setPrice(PRICE);

        when(itemRepository.findByName(NAME)).thenReturn(Optional.of(new Item()));

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> itemService.createItem(dto));

        assertTrue(ex.getMessage().contains(NAME));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void getItems_returnsPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Item item = new Item();
        item.setId(ID);
        item.setName(NAME);
        Page<Item> page = new PageImpl<>(java.util.List.of(item));

        when(itemRepository.findAll(pageable)).thenReturn(page);
        when(itemMapper.toDto(item)).thenReturn(new ItemDto(ID, NAME, PRICE));

        Page<ItemDto> result = itemService.getItems(0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(NAME, result.getContent().get(0).getName());
    }

    @Test
    void getItemById_success() {
        Item item = new Item();
        item.setId(ID);
        item.setName(NAME);

        when(itemRepository.findById(ID)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(new ItemDto(ID, NAME, PRICE));

        ItemDto result = itemService.getItemById(ID);

        assertEquals(NAME, result.getName());
    }

    @Test
    void getItemById_notFound_throwsException() {
        when(itemRepository.findById(ID)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> itemService.getItemById(ID));

        assertTrue(ex.getMessage().contains(ID.toString()));
    }

    @Test
    void getItemByName_success() {
        Item item = new Item();
        item.setId(ID);

        when(itemRepository.findByName(NAME)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(new ItemDto(ID, NAME, PRICE));

        ItemDto result = itemService.getItemByName(NAME);
        assertEquals(NAME, result.getName());
    }

    @Test
    void getItemByName_notFound_throwsException() {
        when(itemRepository.findByName(NAME)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> itemService.getItemByName(NAME));

        assertTrue(ex.getMessage().contains(NAME));
    }

    @Test
    void updateItem_nameNotChanged_success() {
        Item existing = new Item();
        existing.setId(ID);
        existing.setName(NAME);
        existing.setPrice(PRICE);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName(NAME); // то же имя
        dto.setPrice(BigDecimal.valueOf(1499.99));

        when(itemRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(itemRepository.save(existing)).thenReturn(existing);
        when(itemMapper.toDto(existing)).thenReturn(new ItemDto(ID, NAME, dto.getPrice()));

        doNothing().when(itemMapper).updateEntityFromDto(dto, existing);

        ItemDto result = itemService.updateItem(ID, dto);

        assertEquals(dto.getPrice(), result.getPrice());
        verify(itemMapper).updateEntityFromDto(dto, existing);
        verify(itemRepository).save(existing);
        // findByName НЕ должен вызываться, т.к. имя не менялось
        verify(itemRepository, never()).findByName(any());
    }

    @Test
    void updateItem_nameChanged_toUniqueName_success() {
        Item existing = new Item();
        existing.setId(ID);
        existing.setName(NAME);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName(NEW_NAME);

        when(itemRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(itemRepository.findByName(NEW_NAME)).thenReturn(Optional.empty());
        when(itemRepository.save(existing)).thenReturn(existing);

        doNothing().when(itemMapper).updateEntityFromDto(dto, existing);

        itemService.updateItem(ID, dto);

        verify(itemRepository).findByName(NEW_NAME); // проверка уникальности
        verify(itemRepository).save(existing);
    }

    @Test
    void updateItem_nameChanged_toExistingName_throwsException() {
        Item existing = new Item();
        existing.setId(ID);
        existing.setName(NAME);

        Item anotherItem = new Item();
        anotherItem.setId(2L);
        anotherItem.setName(NEW_NAME);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName(NEW_NAME);

        when(itemRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(itemRepository.findByName(NEW_NAME)).thenReturn(Optional.of(anotherItem));

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> itemService.updateItem(ID, dto));

        assertTrue(ex.getMessage().contains(NEW_NAME));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_onlyPriceChanged_success() {
        Item existing = new Item();
        existing.setId(ID);
        existing.setName(NAME);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setPrice(BigDecimal.valueOf(2000));

        when(itemRepository.findById(ID)).thenReturn(Optional.of(existing));
        doNothing().when(itemMapper).updateEntityFromDto(dto, existing);
        when(itemRepository.save(existing)).thenReturn(existing);

        itemService.updateItem(ID, dto);

        verify(itemRepository, never()).findByName(any()); // имя не менялось
        verify(itemRepository).save(existing);
    }

    @Test
    void updateItem_notFound_throwsException() {
        when(itemRepository.findById(ID)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> itemService.updateItem(ID, new ItemUpdateDto()));

        assertTrue(ex.getMessage().contains(ID.toString()));
    }

    @Test
    void deleteItem_success() {
        Item item = new Item();
        item.setId(ID);

        when(itemRepository.findById(ID)).thenReturn(Optional.of(item));

        itemService.deleteItem(ID);

        verify(itemRepository).delete(item);
    }

    @Test
    void deleteItem_notFound_throwsException() {
        when(itemRepository.findById(ID)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> itemService.deleteItem(ID));

        assertTrue(ex.getMessage().contains(ID.toString()));
        verify(itemRepository, never()).delete(any());
    }
}