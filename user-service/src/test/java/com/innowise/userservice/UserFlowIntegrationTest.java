package com.innowise.userservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserWithCardsDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
class UserFlowIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:latest")
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("spring.liquibase.enabled", () -> "true");
  }

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PaymentCardRepository cardRepository;

  @Autowired
  private RedisTemplate<String, UserWithCardsDto> redisTemplate;

  @BeforeEach
  void setUpMockMvc() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
        .build();
  }

  @BeforeEach
  @AfterEach
  void cleanUp() {
    cardRepository.deleteAll();
    userRepository.deleteAll();
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
  }

  @Test
  void shouldCreateUserAddCardAndUseRedisCache() throws Exception {
    UserRequestDto userDto = new UserRequestDto();
    userDto.setUsername("Kirill");
    userDto.setSurname("Masterov");
    userDto.setEmail("kirill@example.com");
    userDto.setBirthDate(LocalDate.of(2006, 12, 1));

    MvcResult userResult = mockMvc.perform(post("/api/v1/users/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.uuid").exists())
        .andReturn();

    String responseBody = userResult.getResponse().getContentAsString();
    String userIdStr = objectMapper.readTree(responseBody).get("uuid").asText();
    UUID userId = UUID.fromString(userIdStr);

    List<User> usersInDb = userRepository.findAll();
    assertThat(usersInDb).hasSize(1);
    assertThat(usersInDb.get(0).getEmail()).isEqualTo("kirill@example.com");

    PaymentCardRequestDto cardDto = new PaymentCardRequestDto();
    cardDto.setUserId(userId);
    cardDto.setCardNumber("1234567812345678");
    cardDto.setHolder("KIRILL MASTEROV");
    cardDto.setExpirationDate("12/26");

    mockMvc.perform(post("/api/v1/cards/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.cardId").exists());

    List<PaymentCard> cardsInDb = cardRepository.findAll();
    assertThat(cardsInDb).hasSize(1);
    assertThat(cardsInDb.get(0).getUser().getId()).isEqualTo(userId);

    mockMvc.perform(get("/api/v1/users/usersWithCards/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.email").value("kirill@example.com"))
        .andExpect(jsonPath("$.cards[0].cardNumber").value("1234567812345678"));

    String cacheKey = "user-info:" + userId;
    Boolean hasCache = redisTemplate.hasKey(cacheKey);
    assertThat(hasCache).isTrue();

    UUID cardId = cardsInDb.get(0).getId();
    mockMvc.perform(delete("/api/v1/cards/delete/" + cardId))
        .andExpect(status().isNoContent());

    Boolean cacheAfterDelete = redisTemplate.hasKey(cacheKey);
    assertThat(cacheAfterDelete).isFalse();
  }
}