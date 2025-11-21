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
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private static final String NEW_NAME = "Gaming Laptop";
    private static final String SEARCH_QUERY = "top";
    private static final BigDecimal PRICE = BigDecimal.valueOf(1299.99);

    @Test
    void createItem_success() {
        var dto = new ItemCreateDto();
        dto.setName(NAME);
        dto.setPrice(PRICE);

        var entity = new Item();
        var saved = new Item();
        saved.setId(ID);
        saved.setName(NAME);
        saved.setPrice(PRICE);

        when(itemRepository.findByName(NAME)).thenReturn(Optional.empty());
        when(itemMapper.toEntity(dto)).thenReturn(entity);
        when(itemRepository.save(entity)).thenReturn(saved);
        when(itemMapper.toDto(saved)).thenReturn(new ItemDto(ID, NAME, PRICE));

        ItemDto result = itemService.createItem(dto);

        assertEquals(ID, result.getId());
        assertEquals(NAME, result.getName());
        verify(itemRepository).save(entity);
    }

    @Test
    void createItem_nameAlreadyExists_throwsException() {
        var dto = new ItemCreateDto();
        dto.setName(NAME);

        when(itemRepository.findByName(NAME)).thenReturn(Optional.of(new Item()));

        var ex = assertThrows(AlreadyExistsException.class, () -> itemService.createItem(dto));
        assertTrue(ex.getMessage().contains(NAME));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void getItems_noFilters_returnsAllPaged() {
        var pageable = PageRequest.of(0, 10);
        var item = item(ID, NAME, PRICE);
        var page = new PageImpl<>(List.of(item), pageable, 1);

        when(itemRepository.findAll(pageable)).thenReturn(page);
        when(itemMapper.toDto(item)).thenReturn(new ItemDto(ID, NAME, PRICE));

        var result = itemService.getItems(0, 10, null);

        assertEquals(1, result.getTotalElements());
        assertEquals(NAME, result.getContent().get(0).getName());
        verify(itemRepository).findAll(pageable);
    }

    @Test
    void getItems_withNameFilter_usesContainingIgnoreCase() {
        var pageable = PageRequest.of(0, 10);
        var item = item(ID, NAME, PRICE);
        var page = new PageImpl<>(List.of(item), pageable, 1);

        when(itemRepository.findByNameContainingIgnoreCase(SEARCH_QUERY, pageable)).thenReturn(page);
        when(itemMapper.toDto(item)).thenReturn(new ItemDto(ID, NAME, PRICE));

        var result = itemService.getItems(0, 10, SEARCH_QUERY);

        assertEquals(1, result.getTotalElements());
        verify(itemRepository).findByNameContainingIgnoreCase(SEARCH_QUERY, pageable);
    }

    @Test
    void getItemById_success() {
        var item = item(ID, NAME, PRICE);

        when(itemRepository.findById(ID)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(new ItemDto(ID, NAME, PRICE));

        var result = itemService.getItemById(ID);

        assertEquals(NAME, result.getName());
    }

    @Test
    void getItemById_notFound_throwsException() {

        when(itemRepository.findById(ID)).thenReturn(Optional.empty());

        var ex = assertThrows(NotFoundException.class, () -> itemService.getItemById(ID));
        assertTrue(ex.getMessage().contains(ID.toString()));
    }

    @Test
    void updateItem_nameNotChanged_success() {
        var existing = item(ID, NAME, PRICE);
        var dto = new ItemUpdateDto();
        dto.setName(NAME);
        dto.setPrice(BigDecimal.valueOf(1499.99));

        when(itemRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(itemRepository.save(existing)).thenReturn(existing);
        when(itemMapper.toDto(existing)).thenReturn(new ItemDto(ID, NAME, dto.getPrice()));
        doNothing().when(itemMapper).updateEntityFromDto(dto, existing);

        var result = itemService.updateItem(ID, dto);

        assertEquals(dto.getPrice(), result.getPrice());
        verify(itemMapper).updateEntityFromDto(dto, existing);
        verify(itemRepository).save(existing);
        verify(itemRepository, never()).findByName(any());
    }

    @Test
    void updateItem_nameChanged_toUnique_success() {
        var existing = item(ID, NAME, PRICE);
        var dto = new ItemUpdateDto();
        dto.setName(NEW_NAME);

        when(itemRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(itemRepository.findByName(NEW_NAME)).thenReturn(Optional.empty()); // свободно
        when(itemRepository.save(existing)).thenReturn(existing);

        doNothing().when(itemMapper).updateEntityFromDto(dto, existing);

        itemService.updateItem(ID, dto);

        verify(itemRepository).findByName(NEW_NAME);
        verify(itemRepository).save(existing);
    }

    @Test
    void updateItem_nameChanged_toExisting_throwsException() {
        var existing = item(ID, NAME, PRICE);
        var anotherItem = item(2L, NEW_NAME, PRICE);

        var dto = new ItemUpdateDto();
        dto.setName(NEW_NAME);

        when(itemRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(itemRepository.findByName(NEW_NAME)).thenReturn(Optional.of(anotherItem));

        var ex = assertThrows(AlreadyExistsException.class, () -> itemService.updateItem(ID, dto));

        assertTrue(ex.getMessage().contains(NEW_NAME));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_onlyPriceChanged_noNameCheck() {
        var existing = item(ID, NAME, PRICE);
        var dto = new ItemUpdateDto();
        dto.setPrice(BigDecimal.valueOf(2000));

        when(itemRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(itemRepository.save(existing)).thenReturn(existing);
        doNothing().when(itemMapper).updateEntityFromDto(dto, existing);

        itemService.updateItem(ID, dto);

        verify(itemRepository, never()).findByName(any());
        verify(itemRepository).save(existing);
    }

    @Test
    void updateItem_notFound_throwsException() {
        when(itemRepository.findById(ID)).thenReturn(Optional.empty());

        var ex = assertThrows(NotFoundException.class, () -> itemService.updateItem(ID, new ItemUpdateDto()));
        assertTrue(ex.getMessage().contains(ID.toString()));
    }

    @Test
    void deleteItem_success() {
        var item = item(ID, NAME, PRICE);

        when(itemRepository.findById(ID)).thenReturn(Optional.of(item));

        itemService.deleteItem(ID);

        verify(itemRepository).delete(item);
    }

    @Test
    void deleteItem_notFound_throwsException() {
        when(itemRepository.findById(ID)).thenReturn(Optional.empty());

        var ex = assertThrows(NotFoundException.class, () -> itemService.deleteItem(ID));
        assertTrue(ex.getMessage().contains(ID.toString()));
        verify(itemRepository, never()).delete(any());
    }

    private Item item(Long id, String name, BigDecimal price) {
        var item = new Item();
        item.setId(id);
        item.setName(name);
        item.setPrice(price);
        return item;
    }
}