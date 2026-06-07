package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.request.ItemCreateRequest;
import com.innowise.orderservice.dto.request.UpdateItemRequest;
import com.innowise.orderservice.dto.response.ItemResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemService {

  /**
   * Creates a new catalog item.
   *
   * @param request item creation request containing name and price
   * @return created item response
   * @throws com.innowise.orderservice.exception.DuplicateEntityException if item with the same name
   *                                                                      already exists
   */
  ItemResponse createItem(ItemCreateRequest request);

  /**
   * Retrieves a paginated list of items with optional filters.
   *
   * @param name     filter by item name (partial match), nullable
   * @param price    filter by exact price, nullable
   * @param pageable pagination and sorting parameters
   * @return page of item responses
   */
  Page<ItemResponse> getAllItems(String name, BigDecimal price, Pageable pageable);

  /**
   * Retrieves an item by its ID.
   *
   * @param itemId item UUID
   * @return item response
   * @throws com.innowise.orderservice.exception.EntityNotFoundException if item is not found
   */
  ItemResponse getItemById(UUID itemId);

  /**
   * Updates an existing item's name and price.
   *
   * @param itemId  item UUID
   * @param request update request containing new name and price
   * @return updated item response
   * @throws com.innowise.orderservice.exception.EntityNotFoundException if item is not found
   */
  ItemResponse updateItem(UUID itemId, UpdateItemRequest request);

  /**
   * Deletes an item by its ID.
   *
   * @param itemId item UUID
   * @throws com.innowise.orderservice.exception.EntityNotFoundException if item is not found
   */
  void deleteItem(UUID itemId);

}
