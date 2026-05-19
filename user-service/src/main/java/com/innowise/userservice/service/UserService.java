package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.dto.UserWithCardsDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.repository.UserSpecification;
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
public class UserService {

  private final UserRepository userRepository;
  private final PaymentCardRepository cardRepository;
  private final UserMapper userMapper;
  private final PaymentCardMapper cardMapper;
  private final RedisTemplate<String, UserWithCardsDto> redisTemplate;

  private static final String CACHE_NAME = "user-info:";

  @Transactional
  public UserResponseDto createUser(UserRequestDto dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new BusinessException("User with email '" + dto.getEmail() + "' already exists");
    }
    User user = userMapper.toEntity(dto);
    user.setActive(true);
    return userMapper.toResponseDto(userRepository.save(user));
  }

  public UserResponseDto getUserById(UUID id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Not found"));

    return userMapper.toResponseDto(user);
  }

  public Page<UserResponseDto> getAllUsers(String name, String surname, Boolean active,
      Pageable pageable) {
    Specification<User> spec = UserSpecification.filterByUsernameAndSurname(name, surname)
        .and(UserSpecification.filterByActive(active));
    return userRepository.findAll(spec, pageable)
        .map(userMapper::toResponseDto);
  }

  public UserWithCardsDto getUsersWithCards(UUID id) {
    var cacheId = CACHE_NAME + id;
    UserWithCardsDto fromCache = redisTemplate.opsForValue().get(cacheId);
    if (fromCache != null) {
      log.info("Get user from cache: {} ", fromCache.getUuid());
      return fromCache;
    }
    log.info("Get user from db: {} ", id);
    User user = findUserOrThrow(id);
    List<PaymentCard> cards = cardRepository.findByUserId(id);
    List<PaymentCardResponseDto> cardsDto = cards.stream()
        .map(cardMapper::toResponseDto)
        .toList();

    UserWithCardsDto responseDto = userMapper.toWithCardsDto(user, cardsDto);
    redisTemplate.opsForValue().set(cacheId, responseDto, Duration.ofMinutes(10));

    return responseDto;
  }

  @Transactional
  public UserResponseDto updateUser(UUID id, UserRequestDto dto) {
    User user = findUserOrThrow(id);
    userMapper.updateUserFromDto(dto, user);

    evictCacheUser(id);

    return userMapper.toResponseDto(userRepository.save(user));
  }

  @Transactional
  public void setActiveStatus(UUID id, Boolean active) {
    findUserOrThrow(id);

    userRepository.setActiveStatus(id, active);
    evictCacheUser(id);
  }

  @Transactional
  public void deleteUser(UUID id) {
    findUserOrThrow(id);
    userRepository.deleteById(id);
    evictCacheUser(id);
  }

  private User findUserOrThrow(UUID id) {
    return userRepository.findById(id)
        .orElseThrow(() -> ResourceNotFoundException.ofUser(id));
  }

  private void evictCacheUser(UUID userId) {
    redisTemplate.delete(CACHE_NAME + userId);
  }

}
