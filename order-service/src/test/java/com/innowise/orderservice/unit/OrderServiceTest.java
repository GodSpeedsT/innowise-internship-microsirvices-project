package com.innowise.orderservice.unit;

import com.innowise.orderservice.config.UserClient;
import com.innowise.orderservice.dao.repository.ItemRepository;
import com.innowise.orderservice.dao.repository.OrderRepository;
import com.innowise.orderservice.dto.request.OrderCreateRequest;
import com.innowise.orderservice.dto.request.OrderItemRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.dto.response.UserResponse;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.exception.EntityNotFoundException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.service.impl.OrderServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;
  @Mock
  private OrderMapper orderMapper;
  @Mock
  private Order order;
  @Mock
  private ItemRepository itemRepository;
  @Mock
  private UserClient userClient;
  @Mock
  private RestClient restClient;
  @Mock
  private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
  @Mock
  private RestClient.RequestHeadersSpec requestHeadersSpec;
  @Mock
  private RestClient.ResponseSpec responseSpec;

  @InjectMocks
  private OrderServiceImpl orderService;

  private UUID orderId;
  private UUID userId;
  private UUID itemId;
  private Item item;
  private OrderCreateRequest orderCreateRequest;
  private OrderResponse orderResponse;
  private UserResponse userResponse;
  private UpdateOrderRequest updateOrderRequest;

  @BeforeEach
  void setUp() {
    orderId = UUID.randomUUID();
    userId = UUID.randomUUID();
    itemId = UUID.randomUUID();

    item = Item.builder()
        .id(itemId)
        .name("iPhone 17")
        .price(BigDecimal.valueOf(3000))
        .build();

    order = Order.builder()
        .id(orderId)
        .userId(userId)
        .totalPrice(BigDecimal.valueOf(6000))
        .status(OrderStatus.NEW)
        .createdAt(LocalDateTime.of(2026, 6, 8, 17, 17))
        .build();

    orderCreateRequest = OrderCreateRequest.builder()
        .userId(userId)
        .orderItems(List.of(new OrderItemRequest(itemId, 2L)))
        .build();

    updateOrderRequest = UpdateOrderRequest.builder()
        .status(OrderStatus.PROCESSING)
        .build();

    userResponse = UserResponse.builder()
        .name("Kirill")
        .surname("Masterov")
        .email("masterov_k@bk.ru")
        .birthday(LocalDate.of(2006, 1, 12))
        .build();

    orderResponse = OrderResponse.builder()
        .orderId(orderId)
        .totalPrice(BigDecimal.valueOf(6000))
        .status(OrderStatus.NEW)
        .build();

  }

  @Test
  @DisplayName("createOrder – success: save order with user data")
  void createOrder_success_withUser() {
    userId = orderCreateRequest.getUserId();

    when(orderMapper.toEntity(orderCreateRequest)).thenReturn(order);
    when(itemRepository.findById(any())).thenReturn(Optional.of(item));
    when(orderRepository.save(any(Order.class))).thenReturn(order);
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    when(userClient.getUserInfo(userId)).thenReturn(userResponse);

    OrderResponse result = orderService.createOrder(orderCreateRequest);

    assertThat(result).isNotNull();
    assertThat(result.getUser()).isEqualTo(userResponse);

    verify(userClient).getUserInfo(userId);
    verify(orderRepository).save(any(Order.class));
  }

  @Test
  @DisplayName("createOrder – success: save order even if user-service is down (fallback returns null)")
  void createOrder_success_whenUserClientReturnsNull() {
    userId = orderCreateRequest.getUserId();

    when(orderMapper.toEntity(orderCreateRequest)).thenReturn(order);
    when(itemRepository.findById(any())).thenReturn(Optional.of(item));
    when(orderRepository.save(any(Order.class))).thenReturn(order);

    orderResponse.setUser(null);
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    when(userClient.getUserInfo(userId)).thenReturn(null);

    OrderResponse result = orderService.createOrder(orderCreateRequest);

    assertThat(result).isNotNull();
    assertThat(result.getUser()).isNull();
    verify(userClient).getUserInfo(userId);
    verify(orderRepository).save(any(Order.class));
  }

  @Test
  @DisplayName("createOrder – throws EntityNotFoundException when item does not exist")
  void createOrder_itemNotFound_throwsException() {
    when(orderMapper.toEntity(orderCreateRequest)).thenReturn(order);
    when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.createOrder(orderCreateRequest))
        .isInstanceOf(EntityNotFoundException.class);

    verify(orderRepository, never()).save(any(Order.class));
  }

  @Test
  @DisplayName("getOrderById – success: returns DTO when item found")
  void getOrderById_success() {
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);
    OrderResponse result = orderService.getOrderById(orderId);

    assertThat(result.getOrderId()).isEqualTo(orderId);
    verify(orderRepository).findById(orderId);
  }

  @Test
  @DisplayName("getOrderById – throws EntityNotFoundException when item not found")
  void getOrderById_notFound_throwsEntityNotFoundException() {
    when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.getOrderById(orderId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("getAllOrders – success: returns page of DTOs")
  void getAllOrders_success() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<Order> orderPage = new PageImpl<>(List.of(order));

    when(orderRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(orderPage);
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);

    Page<OrderResponse> result = orderService.getAllOrders(
        LocalDateTime.of(2026, 6, 8, 17, 17),
        LocalDateTime.of(2026, 6, 10, 10, 0),
        "new",
        pageable);

    assertThat(result.hasContent()).isTrue();
    assertThat(result.getContent().getFirst().getOrderId()).isEqualTo(orderId);
    verify(orderRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  @DisplayName("getAllOrders – returns empty page when no items match filters")
  void getAllOrders_noMatch_returnsEmptyPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    when(orderRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(Page.empty());

    Page<OrderResponse> result = orderService.getAllOrders(
        LocalDateTime.of(2020, 6, 8, 17, 17),
        LocalDateTime.now().plusDays(1),
        "new",
        pageable);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("updateOrder – success: update status, returns DTO")
  void updateOrder_success() {
    UpdateOrderRequest newUpdateDto = UpdateOrderRequest.builder()
        .status(OrderStatus.PROCESSING)
        .build();
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    when(orderMapper.toResponse(order)).thenReturn(orderResponse);

    OrderResponse result = orderService.updateOrder(orderId, newUpdateDto);

    assertThat(result).isNotNull();
    verify(orderMapper).updateOrderFromDto(newUpdateDto, order);
    verify(orderRepository).flush();
  }

  @Test
  @DisplayName("updateOrder – throws EntityNotFoundException when item not found")
  void updateOrder_notFound_throwsEntityNotFoundException() {
    when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.updateOrder(orderId, updateOrderRequest))
        .isInstanceOf(EntityNotFoundException.class);

    verify(orderRepository, never()).save(any());
  }
}

