package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.UserClient;
import com.innowise.orderservice.dao.repository.ItemRepository;
import com.innowise.orderservice.dao.repository.OrderRepository;
import com.innowise.orderservice.dao.specifications.OrderSpecifications;
import com.innowise.orderservice.dto.request.OrderCreateRequest;
import com.innowise.orderservice.dto.request.OrderItemRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.dto.response.UserResponse;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.exception.EntityNotFoundException;
import com.innowise.orderservice.exception.ExternalServiceException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  @Value("${user-service.url}")
  private String userServiceUrl;

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final ItemRepository itemRepository;
  private final RestClient restClient;
  private final UserClient userClient;

  @Transactional
  public OrderResponse createOrder(OrderCreateRequest request) {
    Order order = orderMapper.toEntity(request);

    List<OrderItem> items = createOrderItems(request, order);
    order.setOrderItems(items);
    order.setTotalPrice(calculateTotalPrice(items));
    Order savedOrder = orderRepository.save(order);
    OrderResponse response = orderMapper.toResponse(savedOrder);
    response.setUser(userClient.getUserInfo(request.getUserId()));
    return response;
  }


  @Transactional(readOnly = true)
  public OrderResponse getOrderById(UUID orderId) {
    return orderMapper.toResponse(findOrderOrThrow(orderId));
  }

  @Transactional(readOnly = true)
  public Page<OrderResponse> getAllOrders(LocalDateTime from, LocalDateTime to, String status,
      Pageable pageable) {
    Specification<Order> spec = OrderSpecifications.filterByCreationDate(from, to)
        .and(OrderSpecifications.filterByStatus(status));
    return orderRepository.findAll(spec, pageable)
        .map(orderMapper::toResponse);
  }

  @Transactional
  public OrderResponse updateOrder(UUID orderId, UpdateOrderRequest update) {
    Order order = findOrderOrThrow(orderId);
    orderMapper.updateOrderFromDto(update, order);
    orderRepository.flush();
    return orderMapper.toResponse(order);
  }

  public Page<OrderResponse> getOrdersByUserId(UUID userId, Pageable pageable) {
    return orderRepository.findByUserId(userId, pageable)
        .map(orderMapper::toResponse);
  }

  @Transactional
  public void deleteOrderById(UUID orderId) {
    findOrderOrThrow(orderId);
    orderRepository.deleteById(orderId);
  }

  private List<OrderItem> createOrderItems(OrderCreateRequest request, Order order) {
    return request.getOrderItems().stream()
        .map(itemRequest -> buildOrderItem(itemRequest, order))
        .toList();
  }

  private OrderItem buildOrderItem(OrderItemRequest request, Order order) {
    Item item = itemRepository.findById(request.getItemId())
        .orElseThrow(() -> new EntityNotFoundException("Item", request.getItemId()));
    return OrderItem.builder()
        .order(order)
        .item(item)
        .quantity(request.getQuantity())
        .build();
  }

  private Order findOrderOrThrow(UUID orderId) {
    return orderRepository.findById(orderId)
        .orElseThrow(() -> new EntityNotFoundException("Order not found with id: ", orderId));
  }

  private BigDecimal calculateTotalPrice(List<OrderItem> orderItems) {
    return orderItems.stream()
        .map(orderItem ->
            orderItem.getItem().getPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

}
