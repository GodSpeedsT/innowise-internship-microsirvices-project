package com.innowise.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.CardException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.dao.PaymentCardRepository;
import com.innowise.userservice.dao.UserRepository;
import com.innowise.userservice.service.impl.PaymentCardServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

  @Mock
  private PaymentCardRepository paymentCardRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private PaymentCardMapper paymentCardMapper;
  @Mock
  private RedisTemplate<String, UserResponseDto> redisTemplate;

  @InjectMocks
  private PaymentCardServiceImpl paymentCardService;

  private UUID userId;
  private UUID cardId;
  private User user;
  private PaymentCard card;
  private PaymentCardRequestDto requestDto;
  private PaymentCardResponseDto responseDto;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    cardId = UUID.randomUUID();

    user = User.builder()
        .id(userId)
        .name("Kirill")
        .build();

    card = PaymentCard.builder()
        .id(cardId)
        .user(user)
        .number("1234567890123456")
        .holder("KIRILL MASTEROV")
        .expirationDate(LocalDate.of(2026, 12, 1))
        .active(true)
        .build();

    requestDto = new PaymentCardRequestDto();
    requestDto.setCardNumber("1234567890123456");
    requestDto.setHolder("KIRILL MASTEROV");
    requestDto.setExpirationDate("12/26");

    responseDto = new PaymentCardResponseDto();
    responseDto.setCardId(cardId);
    responseDto.setUserId(userId);
    responseDto.setCardNumber("1234567890123456");
    responseDto.setHolder("KIRILL MASTEROV");
    responseDto.setExpirationDate("12/26");
    responseDto.setActive(true);
  }

  @Test
  @DisplayName("createCard – success: saves card and returns DTO")
  void createCard_success() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.countCardsByUserId(userId)).thenReturn(2L);
    when(paymentCardRepository.findByNumber(requestDto.getCardNumber())).thenReturn(
        Optional.empty());
    when(paymentCardMapper.toEntity(requestDto)).thenReturn(card);
    when(paymentCardRepository.save(card)).thenReturn(card);
    when(paymentCardMapper.toResponseDto(card)).thenReturn(responseDto);
    when(redisTemplate.delete("user-info:" + userId)).thenReturn(true);

    PaymentCardResponseDto result = paymentCardService.createCard(userId, requestDto);

    assertThat(result).isNotNull();
    assertThat(result.getCardNumber()).isEqualTo("1234567890123456");
    verify(paymentCardRepository).save(card);
    verify(redisTemplate).delete("user-info:" + userId);
  }

  @Test
  @DisplayName("createCard – throws CardException when card limit reached 5")
  void createCard_limitReached_throwsCardException() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.countCardsByUserId(userId)).thenReturn(5L);

    assertThatThrownBy(() -> paymentCardService.createCard(userId, requestDto))
        .isInstanceOf(CardException.class)
        .hasMessageContaining("maximum count of cards: 5");

    verify(paymentCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("createCard – throws CardException when card number already exists")
  void createCard_cardNumberExists_throwsCardException() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.countCardsByUserId(userId)).thenReturn(2L);
    when(paymentCardRepository.findByNumber(requestDto.getCardNumber())).thenReturn(
        Optional.of(card));

    assertThatThrownBy(() -> paymentCardService.createCard(userId, requestDto))
        .isInstanceOf(CardException.class)
        .hasMessageContaining("Card number already in use");

    verify(paymentCardRepository, never()).save(any());
  }


  @Test
  @DisplayName("createCard – throws ResourceNotFoundException when user does not exist")
  void createCard_userNotFound_throwsResourceNotFoundException() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.createCard(userId, requestDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User with id " + userId + " not found");
  }

  @Test
  @DisplayName("getCardById – success: returns DTO")
  void getCardById_success() {
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(card));
    when(paymentCardMapper.toResponseDto(card)).thenReturn(responseDto);

    PaymentCardResponseDto result = paymentCardService.getCardById(cardId);

    assertThat(result.getCardId()).isEqualTo(cardId);
  }

  @Test
  @DisplayName("getCardById – throws ResourceNotFoundException when card not found")
  void getCardById_notFound_throwsResourceNotFoundException() {
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.getCardById(cardId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getAllCards – success: returns page of card DTOs")
  void getAllCards_success() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<PaymentCard> cardPage = new PageImpl<>(List.of(card));

    when(paymentCardRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenReturn(cardPage);
    when(paymentCardMapper.toResponseDto(card)).thenReturn(responseDto);

    Page<PaymentCardResponseDto> result = paymentCardService.getAllCards("KIRILL", "MASTEROV", true,
        pageable);

    assertThat(result).hasSize(1);
    assertThat(result.getContent().getFirst().getHolder()).isEqualTo("KIRILL MASTEROV");
  }

  @Test
  @DisplayName("getAllCards – returns empty page when no cards match filter")
  void getAllCards_empty_returnsEmptyPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    when(paymentCardRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenReturn(Page.empty());

    Page<PaymentCardResponseDto> result = paymentCardService.getAllCards(null, null, null,
        pageable);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getCardsByUserId – success: returns list of cards for user")
  void getCardsByUserId_success() {
    when(userRepository.existsById(userId)).thenReturn(true);
    when(paymentCardRepository.findCardsByUserIdNative(userId)).thenReturn(List.of(card));
    when(paymentCardMapper.toResponseDto(card)).thenReturn(responseDto);

    List<PaymentCardResponseDto> result = paymentCardService.getCardsByUserId(userId);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getUserId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("getCardsByUserId – throws ResourceNotFoundException when user not found")
  void getCardsByUserId_userNotFound_throwsResourceNotFoundException() {
    when(userRepository.existsById(userId)).thenReturn(false);

    assertThatThrownBy(() -> paymentCardService.getCardsByUserId(userId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("updateCard – success: updates card fields, evicts cache, returns DTO")
  void updateCard_success() {
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(card));
    when(paymentCardRepository.save(card)).thenReturn(card);
    when(paymentCardMapper.toResponseDto(card)).thenReturn(responseDto);
    when(redisTemplate.delete("user-info:" + userId)).thenReturn(true);

    PaymentCardResponseDto result = paymentCardService.updateCard(cardId, requestDto);

    assertThat(result).isNotNull();
    verify(paymentCardMapper).updateCardFromDto(requestDto, card);
    verify(paymentCardRepository).save(card);
    verify(redisTemplate).delete("user-info:" + userId);
  }

  @Test
  @DisplayName("updateCard – throws ResourceNotFoundException when card not found")
  void updateCard_notFound_throwsResourceNotFoundException() {
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.updateCard(cardId, requestDto))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(paymentCardRepository, never()).save(any());
  }

  @Test
  @DisplayName("setActiveStatus – success: sets status and evicts cache")
  void setActiveStatus_activate_success() {
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(card));
    when(redisTemplate.delete("user-info:" + userId)).thenReturn(true);

    paymentCardService.setActiveStatus(cardId, true);

    verify(paymentCardRepository).setActiveStatus(cardId, true);
    verify(redisTemplate).delete("user-info:" + userId);
  }

  @Test
  @DisplayName("setActiveStatus – throws ResourceNotFoundException when card not found")
  void setActiveStatus_notFound_throwsResourceNotFoundException() {
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.setActiveStatus(cardId, false))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(paymentCardRepository, never()).setActiveStatus(any(), anyBoolean());
  }

  @Test
  @DisplayName("deleteCard – success: deletes card and evicts cache")
  void deleteCard_success() {
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(card));
    when(redisTemplate.delete("user-info:" + userId)).thenReturn(true);

    paymentCardService.deleteCard(cardId);

    verify(paymentCardRepository).deleteById(cardId);
    verify(redisTemplate).delete("user-info:" + userId);
  }

  @Test
  @DisplayName("deleteCard – throws ResourceNotFoundException when card not found")
  void deleteCard_notFound_throwsResourceNotFoundException() {
    when(paymentCardRepository.findById(cardId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.deleteCard(cardId))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(paymentCardRepository, never()).deleteById(any());
  }
}