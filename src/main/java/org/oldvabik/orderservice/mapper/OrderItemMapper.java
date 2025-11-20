package org.oldvabik.orderservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.oldvabik.orderservice.dto.OrderItemDto;
import org.oldvabik.orderservice.entity.OrderItem;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderItemMapper {
    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "name", source = "item.name")
    @Mapping(target = "price", source = "item.price")
    OrderItemDto toDto(OrderItem entity);
}
