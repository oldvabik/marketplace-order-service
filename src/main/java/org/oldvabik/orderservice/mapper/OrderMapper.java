package org.oldvabik.orderservice.mapper;

import org.mapstruct.*;
import org.oldvabik.orderservice.dto.OrderCreateDto;
import org.oldvabik.orderservice.dto.OrderDto;
import org.oldvabik.orderservice.dto.OrderUpdateDto;
import org.oldvabik.orderservice.entity.Order;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {
    @Mapping(target = "user", ignore = true)
    OrderDto toDto(Order entity);

    Order toEntity(OrderCreateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(OrderUpdateDto dto, @MappingTarget Order entity);
}

