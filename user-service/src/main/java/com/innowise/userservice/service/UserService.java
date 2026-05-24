package com.innowise.userservice.service;

import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

  UserResponseDto createUser(UserRequestDto dto);

  UserResponseDto getUserById(UUID id);

  Page<UserResponseDto> getAllUsers(String name, String surname, Boolean active, Pageable pageable);

  UserResponseDto getUsersWithCards(UUID id);

  UserResponseDto updateUser(UUID id, UserRequestDto dto);

  void setActiveStatus(UUID id, Boolean active);

  void deleteUser(UUID id);

}
