package com.innowise.orderservice.integration.messaging.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.innowise.orderservice.dao.repository.ItemRepository;
import com.innowise.orderservice.dao.repository.OrderRepository;
import com.innowise.orderservice.dto.request.OrderCreateRequest;
import com.innowise.orderservice.dto.request.OrderItemRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.integration.BaseIntegrationTest;
import com.innowise.orderservice.messaging.event.OrderCompletedEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

public class OrderProducerIntegrationTest extends BaseIntegrationTest {


  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper mapper;
  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private ItemRepository itemRepository;

  @Test
  @WithMockUser(roles = "USER")
  @DisplayName("Publish CompletedEvent to payment-events")
  void createPayment_publishesPaymentCompletedEvent() throws Exception {

    UUID userId = UUID.randomUUID();
    stubDefaultUser(userId);
    Item item = itemRepository.save(Item.builder()
        .name("test item")
        .price(BigDecimal.valueOf(10000))
        .build());

    List<OrderItemRequest> list = List.of(OrderItemRequest.builder()
        .itemId(item.getId())
        .quantity(3L)
        .build());

    kafkaConsumer.subscribe(List.of("order-events"));

    MvcResult result = mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new OrderCreateRequest(userId, list))))
        .andExpect(status().isCreated())
        .andReturn();

    OrderResponse response = mapper.readValue(result.getResponse().getContentAsString(),
        OrderResponse.class);

    UUID orderId = response.getOrderId();
    BigDecimal expectedTotalPrice = BigDecimal.valueOf(30000);

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      ConsumerRecords<String, OrderCompletedEvent> records = kafkaConsumer.poll(
          Duration.ofSeconds(10));
      assertThat(records).isNotEmpty();
      assertThat(records.iterator().next().value().getOrderId()).isEqualTo(orderId);
      assertThat(records.iterator().next().value().getBillAmount()).isEqualByComparingTo(
          expectedTotalPrice);
    });

  }

}
