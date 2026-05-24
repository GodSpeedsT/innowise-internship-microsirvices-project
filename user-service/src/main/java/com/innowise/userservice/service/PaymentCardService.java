package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentCardService {

  PaymentCardResponseDto createCard(PaymentCardRequestDto dto);

  PaymentCardResponseDto getCardById(UUID id);

  Page<PaymentCardResponseDto> getAllCards(String holder, Boolean active, Pageable pageable);

  List<PaymentCardResponseDto> getCardsByUserId(UUID userId);

  PaymentCardResponseDto updateCard(UUID id, PaymentCardRequestDto dto);

  void setActiveStatus(UUID id, Boolean active);

  void deleteCard(UUID id);
}
