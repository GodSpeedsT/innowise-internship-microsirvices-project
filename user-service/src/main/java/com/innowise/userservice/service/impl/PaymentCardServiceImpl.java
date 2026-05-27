package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.CardException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.dao.PaymentCardRepository;
import com.innowise.userservice.dao.specification.PaymentCardSpecification;
import com.innowise.userservice.dao.UserRepository;
import com.innowise.userservice.service.PaymentCardService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

  private static final int MAX_CARDS_PER_USER = 5;
  private static final String CACHE_NAME = "user-info:";

  private final PaymentCardRepository paymentCardRepository;
  private final UserRepository userRepository;
  private final PaymentCardMapper paymentCardMapper;
  private final RedisTemplate<String, UserResponseDto> redisTemplate;

  @Transactional
  public PaymentCardResponseDto createCard(UUID userId, PaymentCardRequestDto dto) {

    User user = userRepository.findById(userId)
        .orElseThrow(
            () -> new ResourceNotFoundException("User with id " + userId + " not found"));

    long cardCount = userRepository.countCardsByUserId(userId);
    if (cardCount >= MAX_CARDS_PER_USER) {
      throw new CardException("User already has maximum count of cards: " + MAX_CARDS_PER_USER);
    }
    if (paymentCardRepository.findByNumber(dto.getCardNumber()).isPresent()) {
      throw new CardException("Card number already in use: " + dto.getCardNumber());
    }
    PaymentCard card = paymentCardMapper.toEntity(dto);
    card.setUser(user);
    card.setActive(true);

    evictCacheUser(userId);
    return paymentCardMapper.toResponseDto(paymentCardRepository.save(card));
  }

  public PaymentCardResponseDto getCardById(UUID id) {
    return paymentCardMapper.toResponseDto(findCardOrThrow(id));
  }

  public Page<PaymentCardResponseDto> getAllCards(String name, String surname, Boolean active,
      Pageable pageable) {
    Specification<PaymentCard> spec = PaymentCardSpecification.hasUserFirstAndSurname(name, surname)
        .and(PaymentCardSpecification.filterByActive(active));
    return paymentCardRepository.findAll(spec, pageable)
        .map(paymentCardMapper::toResponseDto);
  }

  public List<PaymentCardResponseDto> getCardsByUserId(UUID userId) {
    if (!userRepository.existsById(userId)) {
      throw new ResourceNotFoundException("User with id " + userId + " not found");
    }
    List<PaymentCard> cards = paymentCardRepository.findCardsByUserIdNative(userId);
    return cards.stream()
        .map(paymentCardMapper::toResponseDto)
        .toList();
  }

  @Transactional
  public PaymentCardResponseDto updateCard(UUID id, PaymentCardRequestDto dto) {
    PaymentCard card = findCardOrThrow(id);
    UUID userId = card.getUser().getId();
    paymentCardMapper.updateCardFromDto(dto, card);
    evictCacheUser(userId);
    paymentCardRepository.flush();
    return paymentCardMapper.toResponseDto(card);
  }

  @Transactional
  public void setActiveStatus(UUID id, Boolean active) {
    PaymentCard card = findCardOrThrow(id);
    UUID userId = card.getUser().getId();
    paymentCardRepository.setActiveStatus(id, active);
    evictCacheUser(userId);
  }

  @Transactional
  public void deleteCard(UUID id) {
    PaymentCard card = findCardOrThrow(id);
    paymentCardRepository.deleteById(id);
    evictCacheUser(card.getUser().getId());
  }

  private PaymentCard findCardOrThrow(UUID id) {
    return paymentCardRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Card with id " + id + " not found"));
  }

  private void evictCacheUser(UUID userId) {
    redisTemplate.delete(CACHE_NAME + userId);
  }

}
