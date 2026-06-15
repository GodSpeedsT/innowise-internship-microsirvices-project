package com.innowise.orderservice.integration;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.UUID;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableWireMock(
    @ConfigureWireMock(name = "user-service", port = 8089)
)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  @InjectWireMock("user-service")
  protected WireMockServer wireMockServer;

  private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
      "postgres:latest")
      .withDatabaseName("orders_test")
      .withUsername("test")
      .withPassword("test");

  static {
    postgreSQLContainer.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("user-service.url",
        () -> "http://localhost:8089/api/v1/users/");

  }

  protected void stubUserService(UUID userId, String username, String surname, String email) {
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo("/api/v1/users/" + userId))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {
                      "id" : "%s",
                      "name" : "%s",
                      "surname" : "%s",
                      "email" : "%s"
                    }
                    """.formatted(userId, username, surname, email))));
  }

  protected void stubDefaultUser(UUID userId) {
    stubUserService(userId, "Kirill", "Masterov", "masterov_k@bk.ru");
  }

}
