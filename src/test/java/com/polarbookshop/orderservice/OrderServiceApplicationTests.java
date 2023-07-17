package com.polarbookshop.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.order.web.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.io.IOException;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class OrderServiceApplicationTests {

  @Container
  static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(
      DockerImageName.parse("postgres:14.4"));
  @Autowired
  private WebTestClient webTestClient;
  @Autowired
  private OutputDestination outputDestination;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private BookClient bookClient;

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", OrderServiceApplicationTests::r2dbcUrl);
    registry.add("spring.r2dbc.username", postgresql::getUsername);
    registry.add("spring.r2dbc.password", postgresql::getPassword);
    registry.add("spring.flyway.url", postgresql::getJdbcUrl);
  }

  private static String r2dbcUrl() {
    return String.format("r2dbc:postgresql://%s:%s/%s", postgresql.getHost(),
        postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
        postgresql.getDatabaseName());
  }

  @Test
  void whenGetOrdersThenReturn() throws IOException {
    var bookIsbn = "1234567893";
    Book book = new Book(bookIsbn, "Title", "Author", 9.90);
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
    OrderRequest orderRequest = new OrderRequest(bookIsbn, 1);
    Order expectedOrder = webTestClient
        .post()
        .uri("/orders")
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class)
        .returnResult()
        .getResponseBody();
    assertThat(expectedOrder).isNotNull();
    assertThat(objectMapper.readValue(outputDestination.receive().getPayload(), OrderAcceptedMessage.class))
            .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

    webTestClient
        .get()
        .uri("/orders")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBodyList(Order.class)
        .value(orders -> {
          assertThat(orders.stream().filter(order -> order.bookIsbn().equals(bookIsbn))
              .findAny()).isNotEmpty();
        });
  }

  @Test
  void whenPostRequestAndBookExistsThenOrderAccepted() throws IOException {
    var bookIsbn = "1234567893";
    Book book = new Book(bookIsbn, "Title", "Author", 9.90);
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
    OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

    Order createdOrder = webTestClient
        .post()
        .uri("/orders")
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class)
        .returnResult()
        .getResponseBody();

    assertThat(createdOrder).isNotNull();
    assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
    assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
    assertThat(createdOrder.bookName()).isEqualTo(book.title() + " - " + book.author());
    assertThat(createdOrder.bookPrice()).isEqualTo(book.price());
    assertThat(createdOrder.status()).isEqualTo(OrderStatus.ACCEPTED);

    assertThat(objectMapper.readValue(outputDestination.receive().getPayload(), OrderAcceptedMessage.class))
            .isEqualTo(new OrderAcceptedMessage(createdOrder.id()));
  }

  @Test
  void whenPostRequestAndBookNotExistsThenOrderRejected() {
    var bookIsbn = "1234567893";
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.empty());
    OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

    Order createdOrder = webTestClient
        .post()
        .uri("/orders")
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class)
        .returnResult()
        .getResponseBody();

    assertThat(createdOrder).isNotNull();
    assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
    assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
    assertThat(createdOrder.status()).isEqualTo(OrderStatus.REJECTED);
  }

}
