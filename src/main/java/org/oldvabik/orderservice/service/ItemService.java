package org.oldvabik.orderservice.service;

import org.oldvabik.orderservice.dto.ItemCreateDto;
import org.oldvabik.orderservice.dto.ItemDto;
import org.oldvabik.orderservice.dto.ItemUpdateDto;
import org.springframework.data.domain.Page;

public interface ItemService {
    ItemDto createItem(ItemCreateDto dto);

    Page<ItemDto> getItems(Integer page, Integer size, String name);

    ItemDto getItemById(Long id);

    ItemDto updateItem(Long id, ItemUpdateDto dto);

    void deleteItem(Long id);
}

