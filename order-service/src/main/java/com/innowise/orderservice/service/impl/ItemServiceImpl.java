package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.dao.repository.ItemRepository;
import com.innowise.orderservice.dao.specifications.ItemSpecification;
import com.innowise.orderservice.dto.request.ItemCreateRequest;
import com.innowise.orderservice.dto.response.ItemResponse;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.DuplicateEntityException;
import com.innowise.orderservice.exception.EntityNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.service.ItemService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

  private final ItemRepository itemRepository;
  private final ItemMapper itemMapper;

  @Transactional
  public ItemResponse createItem(ItemCreateRequest itemCreateRequest) {
    if (itemRepository.findByName(itemCreateRequest.getName()).isPresent()) {
      throw new DuplicateEntityException("Item", "name", itemCreateRequest.getName());
    }
    Item item = itemMapper.toEntity(itemCreateRequest);
    return itemMapper.toResponse(itemRepository.save(item));
  }

  public Page<ItemResponse> getAllItems(String name, BigDecimal price, Pageable pageable) {
    Specification<Item> spec = ItemSpecification.filterByName(name)
        .and(ItemSpecification.filterByPrice(price));
    return itemRepository.findAll(spec, pageable)
        .map(itemMapper::toResponse);
  }

  public ItemResponse getItemById(UUID itemId) {
    return itemMapper.toResponse(findItemByIdOrThrow(itemId));
  }

  @Transactional
  public ItemResponse updateItem(UUID itemId, ItemCreateRequest updateItemRequest) {
    Item item = findItemByIdOrThrow(itemId);
    itemMapper.updateItemFromDto(updateItemRequest, item);
    itemRepository.flush();
    return itemMapper.toResponse(item);
  }

  @Transactional
  public void deleteItem(UUID itemId) {
    findItemByIdOrThrow(itemId);
    itemRepository.deleteById(itemId);
  }

  private Item findItemByIdOrThrow(UUID itemId) {
    return itemRepository.findById(itemId)
        .orElseThrow(() -> new EntityNotFoundException("Item not found", itemId));
  }

}
