package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

  @Mapping(source = "user.id", target = "userId")
  PaymentCardResponseDto toResponseDto(PaymentCard paymentCard);

  @Mapping(target = "user", ignore = true)
  PaymentCard toEntity(PaymentCardRequestDto paymentCardRequestDto);

  @Mapping(target = "user", ignore = true)
  void updateCardFromDto(PaymentCardRequestDto paymentCardRequestDto,
      @MappingTarget PaymentCard paymentCard);
}
