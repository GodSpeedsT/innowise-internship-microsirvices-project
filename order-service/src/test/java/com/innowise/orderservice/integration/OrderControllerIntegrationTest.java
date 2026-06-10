package com.innowise.orderservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.innowise.orderservice.dao.repository.ItemRepository;
import com.innowise.orderservice.dao.repository.OrderRepository;
import com.innowise.orderservice.dto.request.OrderCreateRequest;
import com.innowise.orderservice.dto.request.OrderItemRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

class OrderControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private ItemRepository itemRepository;
  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    itemRepository.deleteAll();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createOrder_success() throws Exception {
    Item item = itemRepository.save(Item.builder()
        .name("MacBook Pro")
        .price(BigDecimal.valueOf(2000))
        .build());

    OrderCreateRequest request = OrderCreateRequest.builder()
        .userId(UUID.randomUUID())
        .orderItems(List.of(new OrderItemRequest(item.getId(), 3L)))
        .build();

    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderId").exists())
        .andExpect(jsonPath("$.status").value("NEW"))
        .andExpect(jsonPath("$.totalPrice").value(6000.0))
        .andExpect(jsonPath("$.user").isEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getOrderById_success() throws Exception {
    Order order = orderRepository.save(Order.builder()
        .userId(UUID.randomUUID())
        .totalPrice(BigDecimal.valueOf(1500))
        .status(OrderStatus.NEW)
        .createdAt(LocalDateTime.now())
        .build());

    mockMvc.perform(get("/api/v1/orders/" + order.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value(order.getId().toString()))
        .andExpect(jsonPath("$.status").value("NEW"))
        .andExpect(jsonPath("$.totalPrice").value(1500.0));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getOrderById_notFound() throws Exception {
    UUID randomId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/orders/" + randomId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllOrders_success() throws Exception {
    orderRepository.save(Order.builder()
        .userId(UUID.randomUUID())
        .totalPrice(BigDecimal.valueOf(2500))
        .status(OrderStatus.PROCESSING)
        .createdAt(LocalDateTime.now())
        .build());

    mockMvc.perform(get("/api/v1/orders")
            .param("status", "processing")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].status").value("PROCESSING"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateOrder_success() throws Exception {
    Order order = orderRepository.save(Order.builder()
        .userId(UUID.randomUUID())
        .totalPrice(BigDecimal.valueOf(999))
        .status(OrderStatus.NEW)
        .createdAt(LocalDateTime.now())
        .build());

    UpdateOrderRequest updateRequest = new UpdateOrderRequest(OrderStatus.PROCESSING);

    mockMvc.perform(put("/api/v1/orders/" + order.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PROCESSING"));
  }

}
