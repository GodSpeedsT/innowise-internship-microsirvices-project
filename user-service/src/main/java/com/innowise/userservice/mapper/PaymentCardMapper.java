package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.entity.PaymentCard;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

  @Mapping(source = "id", target = "cardId")
  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "number", target = "cardNumber")
  @Mapping(target = "expirationDate", expression = "java(formatExpirationDate(paymentCard.getExpirationDate()))")
  PaymentCardResponseDto toResponseDto(PaymentCard paymentCard);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(source = "cardNumber", target = "number")
  @Mapping(target = "expirationDate", expression = "java(paymentCardRequestDto.getExpirationDateAsLocalDate())")
  PaymentCard toEntity(PaymentCardRequestDto paymentCardRequestDto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(source = "cardNumber", target = "number")
  @Mapping(target = "expirationDate", expression = "java(paymentCardRequestDto.getExpirationDateAsLocalDate())")
  void updateCardFromDto(PaymentCardRequestDto paymentCardRequestDto,
      @MappingTarget PaymentCard paymentCard);

  default String formatExpirationDate(LocalDate expirationDate) {
    if (expirationDate == null) {
      return null;
    }
    return String.format("%02d/%02d",
        expirationDate.getMonthValue(),
        expirationDate.getYear() % 100);
  }
}
