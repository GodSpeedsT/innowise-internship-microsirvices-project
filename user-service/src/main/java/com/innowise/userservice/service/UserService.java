package com.innowise.userservice.service;

import com.innowise.userservice.dto.UserCreateDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.dto.UserUpdateDto;
import com.innowise.userservice.exception.DuplicateEmailException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

  /**
   * @param dto the user creation data transfer object
   * @return the created user response data
   * @throws DuplicateEmailException if a user with the given email already exists
   */
  UserResponseDto createUser(UserCreateDto dto);

  /**
   * @param id the unique identifier of the user
   * @return the user data
   * @throws ResourceNotFoundException if no user is found with the given id
   */
  UserResponseDto getUserById(UUID id);

  /**
   * @param name     optional filter by user first name
   * @param surname  optional filter by user surname
   * @param active   optional filter by user status
   * @param pageable pagination and sorting information
   * @return a page of user response records
   */
  Page<UserResponseDto> getAllUsers(String name, String surname, Boolean active, Pageable pageable);

  /**
   * Returns user data with their payment cards. Result is cached in Redis.
   *
   * @param id the unique identifier of the user
   * @return user data with embedded list of payment cards
   * @throws ResourceNotFoundException if the user does not exist
   */
  UserResponseDto getUserWithCards(UUID id);

  /**
   * Updates an existing user's information.
   *
   * @param id  the unique identifier of the user to update
   * @param dto the updated data
   * @return the updated user data
   * @throws ResourceNotFoundException if the user does not exist
   */
  UserResponseDto updateUser(UUID id, UserUpdateDto dto);

  /**
   * Activates or deactivates a user account (Soft Delete pattern).
   *
   * @param id       the unique identifier of the user
   * @param activate true to activate, false to deactivate
   * @throws ResourceNotFoundException if the user does not exist
   */
  void setActiveStatus(UUID id, Boolean activate);

  List<UserResponseDto> findUsersByNameAndSurname(String name, String surname);
}
