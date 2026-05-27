package com.innowise.userservice.integraton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.UserCreateDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.dto.UserUpdateDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.dao.PaymentCardRepository;
import com.innowise.userservice.dao.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @Container
  @SuppressWarnings("resource")
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
  private RedisTemplate<String, UserResponseDto> redisTemplate;

  private User savedUser;

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
    Assertions.assertNotNull(redisTemplate.getConnectionFactory());
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    savedUser = null;
  }

  @Test
  void shouldCreateUserAddCardAndUseRedisCache() throws Exception {
    UUID uuid = UUID.randomUUID();
    UserCreateDto userCreateDto = createUserRequest(uuid, "Kirill", "Masterov",
        "kirill@example.com");
    MvcResult userResult = mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userCreateDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andReturn();

    UUID userId = extractUserId(userResult);

    List<User> userInDb = userRepository.findAll();
    assertThat(userInDb).hasSize(1);
    assertThat(userInDb.getFirst().getEmail()).isEqualTo("kirill@example.com");

    PaymentCardRequestDto cardDto = new PaymentCardRequestDto();
    cardDto.setCardNumber("1234567812345678");
    cardDto.setHolder("KIRILL MASTEROV");
    cardDto.setExpirationDate("12/26");

    mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cardDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.cardId").exists());

    List<PaymentCard> cardsInDb = cardRepository.findAll();
    assertThat(cardsInDb).hasSize(1);
    assertThat(cardsInDb.getFirst().getUser().getId()).isEqualTo(userId);

    mockMvc.perform(get("/api/v1/users/{userId}/cards", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("kirill@example.com"))
        .andExpect(jsonPath("$.cards[0].cardNumber").value("1234567812345678"));

    String cacheKey = "user-info:" + userId;
    Boolean hasCache = redisTemplate.hasKey(cacheKey);
    assertThat(hasCache).isTrue();

    UUID cardId = cardsInDb.getFirst().getId();
    mockMvc.perform(delete("/api/v1/cards/" + cardId))
        .andExpect(status().isNoContent());

    Boolean cacheAfterDelete = redisTemplate.hasKey(cacheKey);
    assertThat(cacheAfterDelete).isFalse();

    assertThat(cardRepository.findAll()).isEmpty();
  }

  @Nested
  @DisplayName("User CRUD operations")
  class UserCrudTests {

    @Test
    @DisplayName("Create user with duplicate email")
    void createUserWithDuplicateEmail() throws Exception {
      UUID uuid = UUID.randomUUID();
      UserCreateDto userDto = createUserRequest(uuid, "Kirill", "Masterov", "kirill@example.com");

      mockMvc.perform(post("/api/v1/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(userDto)))
          .andExpect(status().isCreated());
      mockMvc.perform(post("/api/v1/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(userDto)))
          .andExpect(status().isConflict())
          .andExpect(
              jsonPath("$.message").value("User with email 'kirill@example.com' already exists"));
    }

    @Test
    @DisplayName("Get user that not exists")
    void getUserThatNotExists() throws Exception {
      UUID nonExistingUserId = UUID.randomUUID();
      mockMvc.perform(get("/api/v1/users/{id}", nonExistingUserId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("UpdateUser")
    void shouldUpdateUser() throws Exception {
      UUID uuid = UUID.randomUUID();
      UserCreateDto userDto = createUserRequest(uuid, "Kirill", "Masterov", "kirill@example.com");

      MvcResult createResult = mockMvc.perform(post("/api/v1/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(userDto)))
          .andExpect(status().isCreated())
          .andReturn();

      UUID userId = extractUserId(createResult);

      UserUpdateDto updateDto = updateUserRequest("Artem", "Kotenko", "artem@example.com");
      mockMvc.perform(put("/api/v1/users/{id}", userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("Artem"))
          .andExpect(jsonPath("$.surname").value("Kotenko"));

      User updatedUser = userRepository.findById(userId).orElseThrow();
      assertThat(updatedUser.getName()).isEqualTo("Artem");
      assertThat(updatedUser.getSurname()).isEqualTo("Kotenko");
    }

    @Test
    @DisplayName("Activate/deactivate user")
    void shouldActivateAndDeactivateUser() throws Exception {
      UUID uuid = UUID.randomUUID();
      UserCreateDto userDto = createUserRequest(uuid, "Kirill", "Masterov", "kirill@example.com");
      MvcResult createResult = mockMvc.perform(post("/api/v1/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(userDto)))
          .andExpect(status().isCreated())
          .andReturn();

      UUID userId = extractUserId(createResult);

      mockMvc.perform(patch("/api/v1/users/{id}", userId)
              .param("activate", "false"))
          .andExpect(status().isNoContent());

      mockMvc.perform(get("/api/v1/users")
              .param("active", "false"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(userId.toString()));

      mockMvc.perform(patch("/api/v1/users/{id}", userId)
              .param("activate", "true"))
          .andExpect(status().isNoContent());

      mockMvc.perform(get("/api/v1/users")
              .param("active", "true"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(userId.toString()));
    }
  }

  @Nested
  @DisplayName("PaymentCard CRUD operations")
  class PaymentCardCrudTests {

    @BeforeEach
    void createTestUser() throws Exception {
      UUID uuid = UUID.randomUUID();
      UserCreateDto userDto = createUserRequest(uuid, "Card", "Test", "cardtest@example.com");
      MvcResult result = mockMvc.perform(post("/api/v1/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(userDto)))
          .andExpect(status().isCreated())
          .andReturn();
      UUID userId = extractUserId(result);
      savedUser = userRepository.findById(userId).orElseThrow();
    }

    @Test
    @DisplayName("Create 5 cards for user")
    void shouldCreateMaxCardsForUser() throws Exception {
      for (int i = 1; i <= 5; i++) {
        mockMvc.perform(post("/api/v1/users/{userId}/cards", savedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    createCardRequest(
                        String.format("444433332222%d%d%d%d", i, i, i, i),
                        "CARD USER",
                        "12/30"))))
            .andExpect(status().isCreated());
      }

      assertThat(cardRepository.findByUserId(savedUser.getId())).hasSize(5);

      mockMvc.perform(post("/api/v1/users/{userId}/cards", savedUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(
                  createCardRequest("5555444433332222", "CARD USER", "12/30"))))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.message").value("User already has maximum count of cards: 5"));
    }

    @Test
    @DisplayName("Get card by id")
    void shouldGetCardById() throws Exception {
      MvcResult cardResult = mockMvc.perform(post("/api/v1/users/{userId}/cards", savedUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(
                  createCardRequest("1234567890123456", "GET TEST", "12/27"))))
          .andExpect(status().isCreated())
          .andReturn();

      UUID cardId = extractCardId(cardResult);

      mockMvc.perform(get("/api/v1/cards/{id}", cardId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.cardId").value(cardId.toString()))
          .andExpect(jsonPath("$.cardNumber").value("1234567890123456"));
    }


    @Test
    @DisplayName("Update cards and invalidate cache")
    void shouldUpdateCardAndInvalidateCache() throws Exception {
      MvcResult cardResult = mockMvc.perform(post("/api/v1/users/{userId}/cards", savedUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(
                  createCardRequest("9999888877776666", "UPDATE TEST", "12/25"))))
          .andExpect(status().isCreated())
          .andReturn();

      UUID cardId = extractCardId(cardResult);

      mockMvc.perform(get("/api/v1/users/{id}/cards", savedUser.getId()))
          .andExpect(status().isOk());

      String cacheKey = "user-info:" + savedUser.getId();
      assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

      PaymentCardRequestDto updateDto = createCardRequest("1111000022223333", "UPDATED HOLDER",
          "12/28");
      mockMvc.perform(put("/api/v1/cards/{id}", cardId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateDto)))
          .andExpect(status().isOk());

      assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
    }


    @Test
    @DisplayName("Delete cards")
    void shouldDeleteCard() throws Exception {
      MvcResult cardResult = mockMvc.perform(post("/api/v1/users/{userId}/cards", savedUser.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(
                  createCardRequest("7777888899990000", "DELETE TEST", "12/29"))))
          .andExpect(status().isCreated())
          .andReturn();

      UUID cardId = extractCardId(cardResult);
      assertThat(cardRepository.findById(cardId)).isPresent();

      mockMvc.perform(delete("/api/v1/cards/{id}", cardId))
          .andExpect(status().isNoContent());

      assertThat(cardRepository.findById(cardId)).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filters and pagination")
  class FilteringAndPaginationTests {

    @BeforeEach
    void createMultipleUsers() throws Exception {
      for (int i = 1; i <= 15; i++) {
        UUID uuid = UUID.randomUUID();
        UserCreateDto userDto = createUserRequest(
            uuid,
            "User" + i,
            i % 2 == 0 ? "Even" : "Odd",
            "user" + i + "@example.com"
        );
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
            .andExpect(status().isCreated());
      }
    }

    @Test
    @DisplayName("User pagination")
    void shouldPaginateUsers() throws Exception {
      mockMvc.perform(get("/api/v1/users")
              .param("page", "0")
              .param("size", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(5))
          .andExpect(jsonPath("$.totalElements").value(15))
          .andExpect(jsonPath("$.totalPages").value(3))
          .andExpect(jsonPath("$.number").value(0));

      mockMvc.perform(get("/api/v1/users")
              .param("page", "1")
              .param("size", "5"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(5))
          .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    @DisplayName("Filter by name")
    void shouldFilterUsersByName() throws Exception {
      UUID uuid = UUID.randomUUID();
      UserCreateDto uniqueUser = createUserRequest(uuid, "UniqueUser99", "Test",
          "unique@example.com");
      mockMvc.perform(post("/api/v1/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(uniqueUser)))
          .andExpect(status().isCreated());

      mockMvc.perform(get("/api/v1/users")
              .param("name", "UniqueUser99"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Filter by surname")
    void shouldFilterUsersBySurname() throws Exception {
      mockMvc.perform(get("/api/v1/users")
              .param("surname", "Even"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(7));
    }
  }

  @Nested
  @DisplayName("Validate input data")
  class ValidationTests {

    @Test
    @DisplayName("Create user with incorrect data")
    void shouldFailCreateUserWithInvalidData() throws Exception {
      UserCreateDto invalidUser = new UserCreateDto();
      invalidUser.setId(UUID.randomUUID());
      invalidUser.setName("");
      invalidUser.setSurname("Valid");
      invalidUser.setEmail("valid@example.com");
      invalidUser.setBirthDate(LocalDate.of(2000, 1, 1));

      mockMvc.perform(post("/api/v1/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidUser)))
          .andExpect(status().isBadRequest());

      invalidUser.setName("Valid");
      invalidUser.setEmail("invalid-email");

      mockMvc.perform(post("/api/v1/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidUser)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Create card with incorrect data")
    void shouldFailCreateCardWithInvalidData() throws Exception {
      UUID uuid = UUID.randomUUID();
      UserCreateDto userDto = createUserRequest(uuid, "Valid", "User", "valid@example.com");
      MvcResult userResult = mockMvc.perform(post("/api/v1/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(userDto)))
          .andExpect(status().isCreated())
          .andReturn();

      UUID userId = extractUserId(userResult);

      PaymentCardRequestDto invalidCard = new PaymentCardRequestDto();
      invalidCard.setCardNumber("");
      invalidCard.setHolder("HOLDER");
      invalidCard.setExpirationDate("12/26");

      mockMvc.perform(post("/api/v1/users/{id}/cards", userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidCard)))
          .andExpect(status().isBadRequest());
    }
  }

  private UserCreateDto createUserRequest(UUID userId, String name, String surname, String email) {
    UserCreateDto dto = new UserCreateDto();
    dto.setId(userId);
    dto.setName(name);
    dto.setSurname(surname);
    dto.setEmail(email);
    dto.setBirthDate(LocalDate.of(2000, 1, 1));
    return dto;
  }

  private UserUpdateDto updateUserRequest(String name, String surname, String email) {
    UserUpdateDto dto = new UserUpdateDto();
    dto.setName(name);
    dto.setSurname(surname);
    dto.setEmail(email);
    dto.setBirthDate(LocalDate.of(2000, 1, 1));
    return dto;
  }

  private PaymentCardRequestDto createCardRequest(String cardNumber, String holder,
      String expirationDate) {
    PaymentCardRequestDto dto = new PaymentCardRequestDto();
    dto.setCardNumber(cardNumber);
    dto.setHolder(holder);
    dto.setExpirationDate(expirationDate);
    return dto;
  }

  private UUID extractUserId(MvcResult result) throws Exception {
    String responseBody = result.getResponse().getContentAsString();
    String userIdStr = objectMapper.readTree(responseBody).get("id").asString();
    return UUID.fromString(userIdStr);
  }

  private UUID extractCardId(MvcResult result) throws Exception {
    String responseBody = result.getResponse().getContentAsString();
    String cardIdStr = objectMapper.readTree(responseBody).get("cardId").asString();
    return UUID.fromString(cardIdStr);
  }
}