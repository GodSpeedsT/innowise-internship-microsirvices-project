package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.request.ItemCreateRequest;
import com.innowise.orderservice.dto.request.UpdateItemRequest;
import com.innowise.orderservice.dto.response.ItemResponse;
import com.innowise.orderservice.service.ItemService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
public class ItemController {

  private final ItemService itemService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<ItemResponse> createItem(
      @RequestBody @Valid ItemCreateRequest itemCreateRequest) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(itemService.createItem(itemCreateRequest));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{id}")
  public ResponseEntity<ItemResponse> getItemById(@PathVariable UUID id) {
    return ResponseEntity.ok(itemService.getItemById(id));
  }

  @GetMapping
  public ResponseEntity<Page<ItemResponse>> getAllItems(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) BigDecimal price,
      @PageableDefault(sort = "name") Pageable pageable
  ) {
    return ResponseEntity.ok(itemService.getAllItems(name, price, pageable));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public ResponseEntity<ItemResponse> updateItem(@PathVariable UUID id,
      @Valid @RequestBody UpdateItemRequest updateItemRequest) {
    return ResponseEntity.ok(itemService.updateItem(id, updateItemRequest));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<ItemResponse> deleteItem(@PathVariable UUID id) {
    itemService.deleteItem(id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}
