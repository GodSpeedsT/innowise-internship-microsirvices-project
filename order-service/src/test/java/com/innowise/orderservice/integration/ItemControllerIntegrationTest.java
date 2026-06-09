package com.innowise.orderservice.integration;

import com.innowise.orderservice.dao.repository.ItemRepository;
import com.innowise.orderservice.dto.request.ItemCreateRequest;
import com.innowise.orderservice.entity.Item;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private ItemRepository itemRepository;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    itemRepository.deleteAll();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createItem_success() throws Exception {
    ItemCreateRequest request = new ItemCreateRequest("PlayStation 5", BigDecimal.valueOf(500));
    mockMvc.perform(post("/api/v1/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.itemId").exists())
        .andExpect(jsonPath("$.name").value("PlayStation 5"))
        .andExpect(jsonPath("$.price").value(500));
  }

  @Test
  @WithMockUser(roles = "USER")
  void createItem_user_forbidden() throws Exception {
    ItemCreateRequest request = new ItemCreateRequest("PlayStation 5", BigDecimal.valueOf(500));

    mockMvc.perform(post("/api/v1/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isInternalServerError());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getItemById_success() throws Exception {
    ItemCreateRequest request = new ItemCreateRequest("PlayStation 5", BigDecimal.valueOf(500));

    Item item = itemRepository.save(Item.builder()
        .name("PlayStation 5")
        .price(BigDecimal.valueOf(500))
        .build());

    mockMvc.perform(get("/api/v1/items/" + item.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.itemId").value(item.getId().toString()))
        .andExpect(status().isOk());
  }

  @Test
  void getAllItems_success() throws Exception {
    Item item = Item.builder()
        .name("iPhone 15")
        .price(BigDecimal.valueOf(1200))
        .build();
    itemRepository.save(item);

    mockMvc.perform(get("/api/v1/items")
            .param("name", "iPhone 15")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].name").value("iPhone 15"));
  }
}
