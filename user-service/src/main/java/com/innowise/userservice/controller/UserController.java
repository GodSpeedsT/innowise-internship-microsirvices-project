package com.innowise.userservice.controller;

import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.dto.UserWithCardsDto;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  @PostMapping("/create")
  public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto dto) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(userService.createUser(dto));
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @GetMapping("/getAll")
  public ResponseEntity<Page<UserResponseDto>> getAllUsers(
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String surname,
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 10, sort = "username") Pageable pageable
  ) {
    return ResponseEntity.ok(userService.getAllUsers(username, surname, active, pageable));
  }

  @PutMapping("/update/{id}")
  public ResponseEntity<UserResponseDto> updateUser(
      @PathVariable UUID id,
      @Valid @RequestBody UserRequestDto user) {
    return ResponseEntity.ok(userService.updateUser(id, user));
  }

  @GetMapping("/usersWithCards/{userId}")
  public ResponseEntity<UserWithCardsDto> getUsersWithCards(@PathVariable UUID userId) {
    return ResponseEntity.ok(userService.getUsersWithCards(userId));
  }

  @PatchMapping("/activate/{id}")
  public ResponseEntity<Void> activate(@PathVariable UUID id) {
    userService.setActiveStatus(id, true);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/deactivate/{id}")
  public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
    userService.setActiveStatus(id, false);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/delete/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

}
