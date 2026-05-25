package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.exception.CardException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentCardService {

  /**
   * Creates a new payment card and associates it with the specified user. A user may have at most 5
   * cards. If the limit is reached a CardException is thrown. The card number must be globally
   * unique.
   *
   * @param userId the UUID of the user who will own the card
   * @param dto    request DTO containing card number, holder name, and expiration date (MM/YY)
   * @return the created card as a response DTO
   * @throws ResourceNotFoundException if no user with userId exists
   * @throws CardException             if the user already has 5 cards or the card number is already
   *                                   in use
   */
  PaymentCardResponseDto createCard(UUID userId, PaymentCardRequestDto dto);

  /**
   * Retrieves a single payment card by its UUID.
   *
   * @param id the UUID of the card to retrieve
   * @return the found card as a response DTO
   * @throws ResourceNotFoundException if no card with {id} exists
   */
  PaymentCardResponseDto getCardById(UUID id);

  /**
   * Returns a paginated list of all payment cards, optionally filtered by the owning user's
   * first/last name and by the card's active status.
   * <p>
   * Filtering is performed via JPA Specifications. Any combination of parameters may be null, in
   * which case that filter is not applied.
   *
   * @param name     optional partial/full first name of the card owner
   * @param surname  optional partial/full surname of the card owner
   * @param active   optional active flag; null means "no filter"
   * @param pageable pagination and sorting parameters
   * @return a page of matching cards as response DTOs
   */
  Page<PaymentCardResponseDto> getAllCards(String name, String surname, Boolean active,
      Pageable pageable);

  /**
   * Returns all payment cards belonging to the specified user. Uses a named Spring Data method
   * findByUserId.
   *
   * @param userId the UUID of the user
   * @return a list of the user's cards as response DTOs (may be empty)
   * @throws ResourceNotFoundException if no user with userId exists
   */
  List<PaymentCardResponseDto> getCardsByUserId(UUID userId);

  /**
   * Updates an existing payment card with the data from the provided DTO. The card's owner is not
   * changed by this operation. After the update the owning user's Redis cache entry is evicted.
   *
   * @param id  the UUID of the card to update
   * @param dto request DTO with the new card data
   * @return the updated card as a response DTO
   * @throws ResourceNotFoundException if no card with
   */
  PaymentCardResponseDto updateCard(UUID id, PaymentCardRequestDto dto);

  /**
   * Activates or deactivates a payment card. Uses a JPQL @Modifying query. After the status change
   * the owning user's Redis cache entry is evicted.
   *
   * @param id     the UUID of the card
   * @param active true to activate, false to deactivate
   * @throws ResourceNotFoundException if no card with {id} exists
   */
  void setActiveStatus(UUID id, Boolean active);

  /**
   * Permanently deletes a payment card by its UUID. After deletion the owning user's Redis cache
   * entry is evicted.
   *
   * @param id the UUID of the card to delete
   * @throws ResourceNotFoundException if no card with
   */
  void deleteCard(UUID id);
}









