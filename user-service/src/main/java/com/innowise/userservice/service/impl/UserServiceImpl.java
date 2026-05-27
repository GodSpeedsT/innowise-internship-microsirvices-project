package com.innowise.userservice.service.impl;

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
import com.innowise.userservice.dao.specification.UserSpecification;
import com.innowise.userservice.service.UserService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserServiceImpl implements UserService {

  private static final String CACHE_NAME = "user-info:";

  private final UserRepository userRepository;
  private final PaymentCardRepository cardRepository;
  private final UserMapper userMapper;
  private final PaymentCardMapper cardMapper;
  private final RedisTemplate<String, UserResponseDto> redisTemplate;


  @Transactional
  public UserResponseDto createUser(UserCreateDto dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new DuplicateEmailException("User with email '" + dto.getEmail() + "' already exists");
    }

    User user = userMapper.toEntity(dto);
    user.setActive(true);
    return userMapper.toResponseDto(userRepository.save(user));
  }

  public UserResponseDto getUserById(UUID id) {
    return userMapper.toResponseDto(findUserOrThrow(id));
  }

  public List<UserResponseDto> findUsersByNameAndSurname(String name, String surname) {
    List<User> users = userRepository.findUsersByNameAndSurnameNative(name, surname);
    return users.stream()
        .map(userMapper::toResponseDto)
        .toList();
  }

  public Page<UserResponseDto> getAllUsers(String name, String surname, Boolean active,
      Pageable pageable) {
    Specification<User> spec = UserSpecification.filterByNameAndSurname(name, surname)
        .and(UserSpecification.filterByActive(active));
    return userRepository.findAll(spec, pageable)
        .map(userMapper::toResponseDto);
  }

  public UserResponseDto getUserWithCards(UUID id) {
    var cacheId = CACHE_NAME + id;
    UserResponseDto fromCache = redisTemplate.opsForValue().get(cacheId);
    if (fromCache != null) {
      log.info("Get user from cache: {} ", id);
      return fromCache;
    }
    log.info("Get user from db: {} ", id);
    User user = findUserOrThrow(id);
    List<PaymentCard> cards = cardRepository.findByUserId(id);
    List<PaymentCardResponseDto> cardsDto = cards.stream()
        .map(cardMapper::toResponseDto)
        .toList();

    UserResponseDto responseDto = userMapper.toResponseDtoWithCards(user, cardsDto);
    redisTemplate.opsForValue().set(cacheId, responseDto, Duration.ofMinutes(10));

    return responseDto;
  }

  @Transactional
  public UserResponseDto updateUser(UUID id, UserUpdateDto dto) {
    User user = findUserOrThrow(id);
    userMapper.updateUserFromDto(dto, user);
    evictCacheUser(id);
    userRepository.flush();
    return userMapper.toResponseDto(user);
  }

  @Transactional
  public void setActiveStatus(UUID id, Boolean active) {
    findUserOrThrow(id);
    userRepository.setActiveStatus(id, active);
    evictCacheUser(id);
  }

  private User findUserOrThrow(UUID id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User with id '" + id + "' not found"));
  }

  private void evictCacheUser(UUID userId) {
    redisTemplate.delete(CACHE_NAME + userId);
  }

}
