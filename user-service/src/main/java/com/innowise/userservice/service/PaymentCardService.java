package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.dto.UserWithCardsDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.ResourceNotFoundException;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCardService {

  private static final int MAX_CARDS_PER_USER = 5;

  private final PaymentCardRepository paymentCardRepository;
  private final UserRepository userRepository;
  private final PaymentCardMapper paymentCardMapper;
  private final RedisTemplate<String, UserWithCardsDto> redisTemplate;

  private static final String CACHE_NAME = "user-info:";

  @Transactional
  public PaymentCardResponseDto createCard(PaymentCardRequestDto dto) {
    User user = userRepository.findById(dto.getUserId())
        .orElseThrow(() -> ResourceNotFoundException.ofUser(dto.getUserId()));

    long cardCount = userRepository.countCardsByUserId(dto.getUserId());
    if (cardCount >= MAX_CARDS_PER_USER) {
      throw new BusinessException("User already has maximum count of cards: " + MAX_CARDS_PER_USER);
    }
    PaymentCard card = paymentCardMapper.toEntity(dto);
    card.setUser(user);
    card.setActive(true);

    evictCacheUser(dto.getUserId());
    return paymentCardMapper.toResponseDto(paymentCardRepository.save(card));
  }

  public PaymentCardResponseDto getCardById(UUID id) {
    return paymentCardMapper.toResponseDto(findCardOrThrow(id));
  }

  public Page<PaymentCardResponseDto> getAllCards(String holder, Boolean active,
      Pageable pageable) {
    Specification<PaymentCard> spec = PaymentCardSpecification.filterHolder(holder)
        .and(PaymentCardSpecification.filterByActive(active));
    return paymentCardRepository.findAll(spec, pageable)
        .map(paymentCardMapper::toResponseDto);
  }

  public List<PaymentCardResponseDto> getCardsByUserId(UUID userId) {
    if (!userRepository.existsById(userId)) {
      throw ResourceNotFoundException.ofUser(userId);
    }
    return paymentCardRepository.findByUserId(userId).stream()
        .map(paymentCardMapper::toResponseDto)
        .toList();
  }

  @Transactional
  public PaymentCardResponseDto updateCard(UUID id, PaymentCardRequestDto dto) {
    PaymentCard card = findCardOrThrow(id);
    UUID userId = card.getUser().getId();
    paymentCardMapper.updateCardFromDto(dto, card);
    PaymentCardResponseDto response = paymentCardMapper.toResponseDto(
        paymentCardRepository.save(card));

    evictCacheUser(userId);
    return response;
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
        .orElseThrow(() -> ResourceNotFoundException.ofCard(id));
  }

  private void evictCacheUser(UUID userId) {
      redisTemplate.delete(CACHE_NAME + userId);
  }

}
