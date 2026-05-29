package com.innowise.userservice.controller;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.dto.UserCreateDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.dto.UserUpdateDto;
import com.innowise.userservice.service.PaymentCardService;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private final PaymentCardService cardService;

  @PostMapping
  public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserCreateDto dto) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(userService.createUser(dto));
  }

  @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.principal.attributes['userId']")
  @PostMapping("{userId}/cards")
  public ResponseEntity<PaymentCardResponseDto> createCard(
      @PathVariable UUID userId,
      @Valid @RequestBody PaymentCardRequestDto paymentCard) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(cardService.createCard(userId, paymentCard));
  }

  @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.attributes['userId']")
  @GetMapping("/{id}")
  public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<Page<UserResponseDto>> getAllUsers(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String surname,
      @RequestParam(required = false) Boolean active,
      @PageableDefault(sort = "name") Pageable pageable
  ) {
    return ResponseEntity.ok(userService.getAllUsers(name, surname, active, pageable));
  }

  @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.attributes['userId']")
  @GetMapping("/{id}/cards")
  public ResponseEntity<UserResponseDto> getUserWithCards(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.getUserWithCards(id));
  }

  @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.attributes['userId']")
  @PutMapping("/{id}")
  public ResponseEntity<UserResponseDto> updateUser(
      @PathVariable UUID id,
      @Valid @RequestBody UserUpdateDto user) {
    return ResponseEntity.ok(userService.updateUser(id, user));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{id}")
  public ResponseEntity<Void> setActiveStatus(
      @PathVariable UUID id,
      @RequestParam boolean activate) {
    userService.setActiveStatus(id, activate);
    return ResponseEntity.noContent().build();
  }
}
