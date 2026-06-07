package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.request.OrderCreateRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.service.OrderService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
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
@RequestMapping("/api/v1/orders")
public class OrderController {

  private final OrderService orderService;

  @PreAuthorize("hasRole('ADMIN') OR hasRole('USER')")
  @PostMapping
  public ResponseEntity<OrderResponse> createOrder(
      @Valid @RequestBody OrderCreateRequest orderCreateRequest) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(orderService.createOrder(orderCreateRequest));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<Page<OrderResponse>> getOrders(
      @RequestParam(required = false) LocalDateTime from,
      @RequestParam(required = false) LocalDateTime to,
      @RequestParam(required = false) String status,
      @PageableDefault(sort = "status") Pageable pageable
  ) {
    return ResponseEntity.ok(orderService.getAllOrders(from, to, status, pageable));
  }

  @PreAuthorize("hasRole('ADMIN') OR @orderSecurityConfig.isOwner(#id, authentication)")
  @GetMapping("/{id}")
  public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
    return ResponseEntity.ok(orderService.getOrderById(id));
  }

  @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
  @GetMapping("/user/{userId}")
  public ResponseEntity<Page<OrderResponse>> getOrdersByUserId(@PathVariable UUID userId,
      @PageableDefault(sort = "createdAt") Pageable pageable) {
    return ResponseEntity.ok(orderService.getOrdersByUserId(userId, pageable));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public ResponseEntity<OrderResponse> updateOrder(@PathVariable UUID id, @Valid @RequestBody
  UpdateOrderRequest updateOrderRequest) {
    return ResponseEntity.ok(orderService.updateOrder(id, updateOrderRequest));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
    orderService.deleteOrderById(id);
    return ResponseEntity.noContent().build();
  }

}
