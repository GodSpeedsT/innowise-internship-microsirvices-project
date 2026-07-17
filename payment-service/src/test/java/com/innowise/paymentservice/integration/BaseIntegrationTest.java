package com.innowise.paymentservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.paymentservice.messaging.event.OrderCompletedEvent;
import com.innowise.paymentservice.messaging.event.PaymentCompletedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableWireMock(
    @ConfigureWireMock(name = "external-api", port = 8089)
)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  protected KafkaProducer<String, OrderCompletedEvent> kafkaProducer;
  protected KafkaConsumer<String, PaymentCompletedEvent> kafkaConsumer;
  protected UUID orderId;
  protected final String userId = "915a75cb-a998-49ef-b966-710bff82d4a7";
  private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:latest");
  private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("apache/kafka:latest");

  static {
    MONGO_DB_CONTAINER.start();
    KAFKA_CONTAINER.start();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
    registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    registry.add("api.external.url", () ->
        "http://localhost:8089/integers/?num=1&min=1&max=10000&col=1&base=10&format=plain");
  }

  protected void stubExternalApi() {
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    "/integers/?num=1&min=1&max=10000&col=1&base=10&format=plain"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", String.valueOf(MediaType.TEXT_PLAIN))
                .withBody("6768\n")));
  }

  @BeforeEach
  void setup() {
    String bootstrapServers = KAFKA_CONTAINER.getBootstrapServers();

    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
    props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
    kafkaProducer = new KafkaProducer<>(props);

    Map<String, Object> consumerProps = new HashMap<>();
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        JacksonJsonDeserializer.class);
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProps.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, PaymentCompletedEvent.class);
    consumerProps.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
    consumerProps.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    kafkaConsumer = new KafkaConsumer<>(consumerProps);

    orderId = UUID.randomUUID();

  }

}
