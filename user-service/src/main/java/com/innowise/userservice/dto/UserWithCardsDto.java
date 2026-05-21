package com.innowise.userservice.dto;

import java.util.List;
import lombok.Data;

@Data
public class UserWithCardsDto {

  private UserResponseDto user;
  private List<PaymentCardResponseDto> cards;


}
