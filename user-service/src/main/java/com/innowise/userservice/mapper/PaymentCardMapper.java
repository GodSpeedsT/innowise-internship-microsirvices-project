package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

  @Mapping(source = "id", target = "cardId")
  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "number", target = "cardNumber")
  PaymentCardResponseDto toResponseDto(PaymentCard paymentCard);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(source = "cardNumber", target = "number")
  PaymentCard toEntity(PaymentCardRequestDto paymentCardRequestDto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(source = "cardNumber", target = "number")
  void updateCardFromDto(PaymentCardRequestDto paymentCardRequestDto,
      @MappingTarget PaymentCard paymentCard);
}
