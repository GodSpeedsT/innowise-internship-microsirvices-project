package com.innowise.userservice.service;

import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.repository.UserSpecification;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Transactional
  public UserResponseDto createUser(UserRequestDto dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new IllegalArgumentException("Email already exists");
    }
    User user = userMapper.toEntity(dto);
    user.setActive(true);
    return userMapper.toResponseDto(userRepository.save(user));
  }

  public UserResponseDto getUserById(UUID id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found: " + id));

    return userMapper.toResponseDto(user);
  }

  public Page<UserResponseDto> getAllUsers(String name, String surname, Boolean active,
      Pageable pageable) {
    Specification<User> spec = UserSpecification.filterByUsernameAndSurname(name, surname)
        .and(UserSpecification.filterByActive(active));
    return userRepository.findAll(spec, pageable)
        .map(userMapper::toResponseDto);
  }

  @Transactional
  public UserResponseDto updateUser(UUID id, UserRequestDto dto) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found: " + id));
    userMapper.updateUserFromDto(dto, user);
    return userMapper.toResponseDto(userRepository.save(user));
  }

  @Transactional
  public void setActiveStatus(UUID id, Boolean active) {
    if (!userRepository.existsById(id)) {
      throw new RuntimeException("User not found: " + id);
    }
    userRepository.setActiveStatus(id, active);
  }

  @Transactional
  public void deleteUser(UUID id) {
    if (!userRepository.existsById(id)) {
      throw new RuntimeException("User not found: " + id);
    }
    userRepository.deleteById(id);
  }

}
