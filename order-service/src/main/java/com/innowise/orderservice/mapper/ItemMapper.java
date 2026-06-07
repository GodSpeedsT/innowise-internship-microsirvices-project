package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.request.ItemCreateRequest;
import com.innowise.orderservice.dto.request.UpdateItemRequest;
import com.innowise.orderservice.dto.response.ItemResponse;
import com.innowise.orderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Item toEntity(ItemCreateRequest request);

  @Mapping(source = "id", target = "itemId")
  ItemResponse toResponse(Item item);

  void updateItemFromDto(UpdateItemRequest request, @MappingTarget Item item);

}
