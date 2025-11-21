package org.oldvabik.orderservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.oldvabik.orderservice.dto.ItemCreateDto;
import org.oldvabik.orderservice.dto.ItemDto;
import org.oldvabik.orderservice.dto.ItemUpdateDto;
import org.oldvabik.orderservice.entity.Item;
import org.oldvabik.orderservice.exception.AlreadyExistsException;
import org.oldvabik.orderservice.exception.NotFoundException;
import org.oldvabik.orderservice.mapper.ItemMapper;
import org.oldvabik.orderservice.repository.ItemRepository;
import org.oldvabik.orderservice.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemServiceImpl(ItemRepository itemRepository,
                           ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    @Override
    @Transactional
    public ItemDto createItem(ItemCreateDto dto) {
        log.debug("[ItemService] createItem: name={}", dto.getName());

        itemRepository.findByName(dto.getName()).ifPresent(i -> {
            log.warn("[ItemService] createItem: name={} already exists", dto.getName());
            throw new AlreadyExistsException("item with name " + dto.getName() + " already exists");
        });

        Item item = itemMapper.toEntity(dto);
        Item savedItem = itemRepository.save(item);

        log.info("[ItemService] createItem: created id={}", savedItem.getId());
        return itemMapper.toDto(savedItem);
    }

    @Override
    public Page<ItemDto> getItems(Integer page, Integer size, String name) {
        log.debug("[ItemService] getItems: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Item> items;

        if (name != null && !name.isBlank()) {
            items = itemRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            items = itemRepository.findAll(pageable);
        }

        log.info("[ItemService] getItems: fetched {} items", items.getContent().size());
        return items.map(itemMapper::toDto);
    }

    @Override
    public ItemDto getItemById(Long id) {
        log.debug("[ItemService] getItemById: id={}", id);

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[ItemService] getItemById: id={} not found", id);
                    return new NotFoundException("item with id " + id + " not found");
                });

        log.info("[ItemService] getItemById: found id={}", id);
        return itemMapper.toDto(item);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long id, ItemUpdateDto dto) {
        log.debug("[ItemService] updateItem: id={}", id);

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[ItemService] updateItem: id={} not found", id);
                    return new NotFoundException("item with id " + id + " not found");
                });

        if (dto.getName() != null && !dto.getName().equals(item.getName())) {
            itemRepository.findByName(dto.getName()).ifPresent(i -> {
                log.warn("[ItemService] updateItem: name={} already exists", dto.getName());
                throw new AlreadyExistsException("item with name " + dto.getName() + " already exists");
            });
        }

        itemMapper.updateEntityFromDto(dto, item);
        Item updatedItem = itemRepository.save(item);

        log.info("[ItemService] updateItem: updated id={}", id);
        return itemMapper.toDto(updatedItem);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        log.debug("[ItemService] deleteItem: id={}", id);

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[ItemService] deleteItem: id={} not found", id);
                    return new NotFoundException("item with id " + id + " not found");
                });

        itemRepository.delete(item);

        log.info("[ItemService] deleteItem: deleted id={}", id);
    }
}
