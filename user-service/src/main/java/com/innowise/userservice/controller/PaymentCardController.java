package com.innowise.userservice.controller;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import java.util.List;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/cards")
public class PaymentCardController {

  private final PaymentCardService paymentCardService;

  @PostMapping
  public ResponseEntity<PaymentCardResponseDto> createCard(
      @Valid @RequestBody PaymentCardRequestDto paymentCard) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(paymentCardService.createCard(paymentCard));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PaymentCardResponseDto> getCardById(@PathVariable UUID id) {
    return ResponseEntity.ok(paymentCardService.getCardById(id));
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<List<PaymentCardResponseDto>> getCardByUserId(@PathVariable UUID userId) {
    return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId));
  }

  @GetMapping
  public ResponseEntity<Page<PaymentCardResponseDto>> getAllCards(
      @RequestParam(required = false) String holder,
      @RequestParam(required = false) Boolean active,
      @PageableDefault Pageable pageable
  ) {
    return ResponseEntity.ok(paymentCardService.getAllCards(holder, active, pageable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<PaymentCardResponseDto> updateCard(
      @PathVariable UUID id,
      @Valid @RequestBody PaymentCardRequestDto paymentCard) {
    return ResponseEntity.ok(paymentCardService.updateCard(id, paymentCard));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> setActivateStatus(@PathVariable UUID id,
      @RequestParam Boolean activate) {
    paymentCardService.setActiveStatus(id, activate);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCard(@PathVariable UUID id) {
    paymentCardService.deleteCard(id);
    return ResponseEntity.noContent().build();
  }
}
