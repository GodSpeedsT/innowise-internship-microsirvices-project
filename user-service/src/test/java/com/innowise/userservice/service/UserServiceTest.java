package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.dto.UserCreateDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.dto.UserUpdateDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.DuplicateEmailException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.dao.PaymentCardRepository;
import com.innowise.userservice.dao.UserRepository;
import com.innowise.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private PaymentCardRepository paymentCardRepository;
  @Mock
  private UserMapper userMapper;
  @Mock
  private PaymentCardMapper paymentCardMapper;
  @Mock
  private RedisTemplate<String, UserResponseDto> redisTemplate;
  @Mock
  private ValueOperations<String, UserResponseDto> valueOperations;

  @InjectMocks
  private UserServiceImpl userService;

  private UUID userId;
  private User user;
  private UserCreateDto requestDto;
  private UserResponseDto responseDto;
  private UserUpdateDto updateDto;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();

    user = User.builder()
        .id(userId)
        .name("Kirill")
        .surname("Masterov")
        .email("masterov_k@bk.ru")
        .birthDate(LocalDate.of(2000, 1, 12))
        .active(true)
        .build();

    requestDto = new UserCreateDto();
    requestDto.setName("Kirill");
    requestDto.setSurname("Masterov");
    requestDto.setEmail("masterov_k@bk.ru");
    requestDto.setBirthDate(LocalDate.of(2000, 1, 12));

    updateDto = new UserUpdateDto();
    updateDto.setName("Kirill");
    updateDto.setSurname("Masterov");
    updateDto.setEmail("masterov_k@bk.ru");
    updateDto.setBirthDate(LocalDate.of(2000, 1, 12));


    responseDto = new UserResponseDto();
    responseDto.setId(userId);
    responseDto.setName("Kirill");
    responseDto.setSurname("Masterov");
    responseDto.setEmail("masterov_k@bk.ru");
    responseDto.setActive(true);
  }

  @Test
  @DisplayName("createUser – success: saves user and returns DTO")
  void createUser_success() {
    when(userRepository.existsByEmail(requestDto.getEmail())).thenReturn(false);
    when(userMapper.toEntity(requestDto)).thenReturn(user);
    when(userRepository.save(user)).thenReturn(user);
    when(userMapper.toResponseDto(user)).thenReturn(responseDto);

    UserResponseDto result = userService.createUser(requestDto);

    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo("masterov_k@bk.ru");
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("createUser – throws DuplicateEmailException when email already exists")
  void createUser_emailAlreadyExists_throwsDuplicateEmailException() {
    when(userRepository.existsByEmail(requestDto.getEmail())).thenReturn(true);

    assertThatThrownBy(() -> userService.createUser(requestDto))
        .isInstanceOf(DuplicateEmailException.class)
        .hasMessageContaining("already exists");

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("getUserById – success: returns DTO when user found")
  void getUserById_success() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toResponseDto(user)).thenReturn(responseDto);

    UserResponseDto result = userService.getUserById(userId);

    assertThat(result.getId()).isEqualTo(userId);
    verify(userRepository).findById(userId);
  }

  @Test
  @DisplayName("getUserById – throws ResourceNotFoundException when user not found")
  void getUserById_notFound_throwsResourceNotFoundException() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserById(userId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getAllUsers – success: returns page of DTOs")
  void getAllUsers_success() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<User> userPage = new PageImpl<>(List.of(user));

    when(userRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(userPage);
    when(userMapper.toResponseDto(user)).thenReturn(responseDto);

    Page<UserResponseDto> result = userService.getAllUsers("Kirill", null, true, pageable);

    assertThat(result.hasContent()).isTrue();
    assertThat(result.getContent().getFirst().getName()).isEqualTo("Kirill");
    verify(userRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  @DisplayName("getAllUsers – returns empty page when no users match filters")
  void getAllUsers_noMatch_returnsEmptyPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    when(userRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(Page.empty());

    Page<UserResponseDto> result = userService.getAllUsers("Unknown", null, null, pageable);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("getUsersWithCards – success: fetches from DB and caches result")
  void getUsersWithCards_cacheEmpty_fetchesFromDbAndCaches() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("user-info:" + userId)).thenReturn(null);

    PaymentCard card = new PaymentCard();
    PaymentCardResponseDto cardDto = new PaymentCardResponseDto();
    cardDto.setCardId(UUID.randomUUID());
    cardDto.setUserId(userId);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(paymentCardRepository.findByUserId(userId)).thenReturn(List.of(card));
    when(paymentCardMapper.toResponseDto(card)).thenReturn(cardDto);
    when(userMapper.toResponseDtoWithCards(eq(user), anyList())).thenReturn(responseDto);
    responseDto.setCards(List.of(cardDto));

    UserResponseDto result = userService.getUserWithCards(userId);

    assertThat(result).isNotNull();
    assertThat(result.getCards()).hasSize(1);
    verify(valueOperations).set(eq("user-info:" + userId), any(UserResponseDto.class), any());
  }

  @Test
  @DisplayName("getUserWithCards – returns cached value without hitting DB")
  void getUsersWithCards_cacheHit_returnsCachedValue() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("user-info:" + userId)).thenReturn(responseDto);

    UserResponseDto result = userService.getUserWithCards(userId);

    assertThat(result).isSameAs(responseDto);
    verify(userRepository, never()).findById(any());
  }

  @Test
  @DisplayName("getUserWithCards – throws ResourceNotFoundException when user not found")
  void getUsersWithCards_userNotFound_throwsException() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("user-info:" + userId)).thenReturn(null);
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserWithCards(userId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("updateUser – success: updates fields, evicts cache, returns DTO")
  void updateUser_success() {
    UserUpdateDto newUpdateDto = new UserUpdateDto();
    newUpdateDto.setName("Artem");
    newUpdateDto.setSurname("Kotenko");
    newUpdateDto.setEmail("artem.ko@mail.ru");
    newUpdateDto.setBirthDate(LocalDate.of(2000, 1, 1));

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toResponseDto(user)).thenReturn(responseDto);
    when(redisTemplate.delete("user-info:" + userId)).thenReturn(true);

    UserResponseDto result = userService.updateUser(userId, newUpdateDto);

    assertThat(result).isNotNull();
    verify(userMapper).updateUserFromDto(newUpdateDto, user);
    verify(userRepository).flush();
    verify(redisTemplate).delete("user-info:" + userId);
  }

  @Test
  @DisplayName("updateUser – throws ResourceNotFoundException when user not found")
  void updateUser_notFound_throwsResourceNotFoundException() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateUser(userId, updateDto))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("setActiveStatus – success: calls repository with correct args and evicts cache")
  void setActiveStatus_success() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    doNothing().when(userRepository).setActiveStatus(userId, false);
    when(redisTemplate.delete("user-info:" + userId)).thenReturn(true);

    userService.setActiveStatus(userId, false);

    verify(userRepository).setActiveStatus(userId, false);
    verify(redisTemplate).delete("user-info:" + userId);
  }

  @Test
  @DisplayName("setActiveStatus – throws ResourceNotFoundException when user not found")
  void setActiveStatus_notFound_throwsResourceNotFoundException() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.setActiveStatus(userId, true))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(userRepository, never()).setActiveStatus(any(), anyBoolean());
  }
}