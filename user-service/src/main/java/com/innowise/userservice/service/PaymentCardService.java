package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.PaymentCardSpecification;
import com.innowise.userservice.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCardService {

  private static final int MAX_CARDS_PER_USER = 5;

  private final PaymentCardRepository paymentCardRepository;
  private final UserRepository userRepository;
  private final PaymentCardMapper paymentCardMapper;

  @Transactional
  public PaymentCardResponseDto createCard(PaymentCardRequestDto dto) {
    User user = userRepository.findById(dto.getUserId())
        .orElseThrow(() -> new RuntimeException("User not found: " + dto.getUserId()));

    long cardCount = userRepository.countCardsByUserId(dto.getUserId());
    if (cardCount >= MAX_CARDS_PER_USER) {
      throw new RuntimeException("Card count exceeds limit: " + MAX_CARDS_PER_USER);
    }
    PaymentCard card = paymentCardMapper.toEntity(dto);
    card.setUser(user);
    card.setActive(true);

    return paymentCardMapper.toResponseDto(paymentCardRepository.save(card));
  }

  public Page<PaymentCardResponseDto> getCards(String holder, Boolean active, Pageable pageable) {
    Specification<PaymentCard> spec = PaymentCardSpecification.filterHolder(holder)
        .and(PaymentCardSpecification.filterByActive(active));
    return paymentCardRepository.findAll(spec, pageable)
        .map(paymentCardMapper::toResponseDto);
  }

  public List<PaymentCardResponseDto> getAllCards(UUID userId) {
    return paymentCardRepository.findByUserId(userId)
        .stream()
        .map(paymentCardMapper::toResponseDto)
        .toList();
  }

  @Transactional
  public PaymentCardResponseDto updateCard(UUID id, PaymentCardRequestDto dto) {
    PaymentCard card = paymentCardRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Card not found: " + id));
    paymentCardMapper.updateCardFromDto(dto, card);
    return paymentCardMapper.toResponseDto(paymentCardRepository.save(card));
  }

  @Transactional
  public void setActiveStatus(UUID id, Boolean active) {
    if (!paymentCardRepository.existsById(id)) {
      throw new RuntimeException("Card not found: " + id);
    }
    paymentCardRepository.setActiveStatus(id, active);
  }

  @Transactional
  public void deleteCard(UUID id) {
    if (!paymentCardRepository.existsById(id)) {
      throw new RuntimeException("Card not found: " + id);
    }
    paymentCardRepository.deleteById(id);
  }

}
