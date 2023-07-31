package com.polarbookshop.orderservice.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.polarbookshop.orderservice.config.SecurityConfig;
import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderService;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerWebFluxTests {

  @Autowired
  WebTestClient webTestClient;

  @MockBean
  OrderService orderService;

  @MockBean
  ReactiveJwtDecoder reactiveJwtDecoder;

  @Test
  void whenBookNotAvailableThenRejectOrder() {
    var orderRequest = new OrderRequest("1234567890", 3);
    var rejectedOrder = OrderService.buildRejectedOrder(orderRequest.isbn(),
        orderRequest.quantity());
    given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity()))
        .willReturn(Mono.just(rejectedOrder));

    webTestClient
        .mutateWith(SecurityMockServerConfigurers.mockJwt()
            .authorities(new SimpleGrantedAuthority("ROLE_customer")))
        .post()
        .uri("/orders")
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(Order.class).value(actualOrder -> {
          assertThat(actualOrder).isNotNull();
          assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
        });
  }
}