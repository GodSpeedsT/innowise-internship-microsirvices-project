package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.request.OrderCreateRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", imports = {OrderStatus.class})
public interface OrderMapper {

  @Mapping(target = "status", expression = "java(OrderStatus.NEW)")
  @Mapping(target = "deleted", constant = "false")
  @Mapping(target = "orderItems", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "totalPrice", ignore = true)
  Order toEntity(OrderCreateRequest orderCreate);

  @Mapping(source = "id", target = "orderId")
  @Mapping(target = "user", ignore = true)
  OrderResponse toResponse(Order order);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "totalPrice", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "orderItems", ignore = true)
  @Mapping(source = "status", target = "status")
  void updateOrderFromDto(UpdateOrderRequest dto, @MappingTarget Order order);

}
