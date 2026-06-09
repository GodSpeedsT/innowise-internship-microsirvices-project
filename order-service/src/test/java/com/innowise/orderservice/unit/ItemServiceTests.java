package com.innowise.orderservice.unit;

import com.innowise.orderservice.dao.repository.ItemRepository;
import com.innowise.orderservice.dto.request.ItemCreateRequest;
import com.innowise.orderservice.dto.response.ItemResponse;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.DuplicateEntityException;
import com.innowise.orderservice.exception.EntityNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.service.impl.ItemServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTests {

  @Mock
  private ItemRepository itemRepository;
  @Mock
  private ItemMapper itemMapper;
  @Mock
  private Item item;
  @InjectMocks
  private ItemServiceImpl itemService;

  private UUID itemId;
  private ItemCreateRequest itemCreateRequest;
  private ItemResponse itemResponse;

  @BeforeEach
  void setUp() {
    itemId = UUID.randomUUID();

    item = Item.builder()
        .id(itemId)
        .name("Samsung Galaxy")
        .price(BigDecimal.valueOf(3000))
        .build();

    itemCreateRequest = ItemCreateRequest.builder()
        .name("Samsung Galaxy")
        .price(BigDecimal.valueOf(3000))
        .build();

    itemCreateRequest = ItemCreateRequest.builder()
        .name("Samsung Galaxy")
        .price(BigDecimal.valueOf(3500))
        .build();

    itemResponse = ItemResponse.builder()
        .itemId(itemId)
        .name("Samsung Galaxy")
        .price(BigDecimal.valueOf(3000))
        .build();
  }

  @Test
  void createItem_success() {
    when(itemRepository.findByName(itemCreateRequest.getName()))
        .thenReturn(Optional.empty());
    when(itemMapper.toEntity(itemCreateRequest)).thenReturn(item);
    when(itemRepository.save(item)).thenReturn(item);
    when(itemMapper.toResponse(item)).thenReturn(itemResponse);

    ItemResponse result = itemService.createItem(itemCreateRequest);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Samsung Galaxy");
    verify(itemRepository).save(item);
  }

  @Test
  @DisplayName("createItem – throws DuplicateEntityException when item name already exists")
  void createItem_itemAlreadyExists_throwsDuplicateEntityException() {
    when(itemRepository.findByName(itemCreateRequest.getName()))
        .thenReturn(Optional.of(new Item()));
    assertThatThrownBy(() -> itemService.createItem(itemCreateRequest))
        .isInstanceOf(DuplicateEntityException.class)
        .hasMessageContaining("already exists");

    verify(itemRepository, never()).save(any());
  }

  @Test
  @DisplayName("getItemById – success: returns DTO when item found")
  void getItemById_success() {
    when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
    when(itemMapper.toResponse(item)).thenReturn(itemResponse);
    ItemResponse result = itemService.getItemById(itemId);

    assertThat(result.getItemId()).isEqualTo(itemId);
    verify(itemRepository).findById(itemId);
  }

  @Test
  @DisplayName("getItemById – throws EntityNotFoundException when item not found")
  void getItemById_notFound_throwsEntityNotFoundException() {
    when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> itemService.getItemById(itemId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("getAllItems – success: returns page of DTOs")
  void getAllItems_success() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<Item> itemPage = new PageImpl<>(List.of(item));

    when(itemRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(itemPage);
    when(itemMapper.toResponse(item)).thenReturn(itemResponse);

    Page<ItemResponse> result = itemService.getAllItems("Samsung Galaxy", BigDecimal.valueOf(3000),
        pageable);

    assertThat(result.hasContent()).isTrue();
    assertThat(result.getContent().getFirst().getName()).isEqualTo("Samsung Galaxy");
    verify(itemRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  @DisplayName("getAllItems – returns empty page when no items match filters")
  void getAllItems_noMatch_returnsEmptyPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    when(itemRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(Page.empty());

    Page<ItemResponse> result = itemService.getAllItems("Unknown", null, pageable);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("updateItem – success: updates fields, returns DTO")
  void updateItem_success() {
    ItemCreateRequest newUpdateDto = ItemCreateRequest.builder()
        .name("Samsung Galaxy")
        .price(BigDecimal.valueOf(3500))
        .build();
    when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
    when(itemMapper.toResponse(item)).thenReturn(itemResponse);

    ItemResponse result = itemService.updateItem(itemId, newUpdateDto);

    assertThat(result).isNotNull();
    verify(itemMapper).updateItemFromDto(newUpdateDto, item);
    verify(itemRepository).flush();
  }

  @Test
  @DisplayName("updateItem – throws EntityNotFoundException when item not found")
  void updateItem_notFound_throwsEntityNotFoundException() {
    when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> itemService.updateItem(itemId, itemCreateRequest))
        .isInstanceOf(EntityNotFoundException.class);

    verify(itemRepository, never()).save(any());
  }
}
