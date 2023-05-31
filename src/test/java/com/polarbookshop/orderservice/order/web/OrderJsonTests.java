package com.polarbookshop.orderservice.order.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
public class OrderJsonTests {

  @Autowired
  private JacksonTester<Order> json;

  @Test
  void testSerialize() throws IOException {
    var order = new Order(394L, "1234567890", "Book Name", 9.90, 1, OrderStatus.ACCEPTED,
        Instant.now(), Instant.now(), 1);
    var jsonContent = json.write(order);
    assertThat(jsonContent).extractingJsonPathNumberValue("@.id").isEqualTo(order.id().intValue());
    assertThat(jsonContent).extractingJsonPathStringValue("@.bookIsbn").isEqualTo(order.bookIsbn());
    assertThat(jsonContent).extractingJsonPathStringValue("@.bookName").isEqualTo(order.bookName());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.bookPrice")
        .isEqualTo(order.bookPrice());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.quantity").isEqualTo(order.quantity());
    assertThat(jsonContent).extractingJsonPathStringValue("@.status")
        .isEqualTo(order.status().name());
    assertThat(jsonContent).extractingJsonPathStringValue("@.createdDate")
        .isEqualTo(order.createdDate().toString());
    assertThat(jsonContent).extractingJsonPathStringValue("@.lastModifiedDate")
        .isEqualTo(order.lastModifiedDate().toString());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.version").isEqualTo(order.version());
  }

  @Test
  void testDeserialize() throws IOException {
    var instant = Instant.parse("2023-05-30T18:46:19.868175Z");
    var jsonContent = """
        {
          "id": 394,
          "bookIsbn": "1234567890",
          "bookName": "Book Name",
          "bookPrice": 9.90,
          "quantity": 1,
          "status": "ACCEPTED",
          "createdDate": "2023-05-30T18:46:19.868175Z",
          "lastModifiedDate": "2023-05-30T18:46:19.868175Z",
          "version": 21
        }
        """;
    assertThat(json.parse(jsonContent))
        .usingRecursiveComparison()
        .isEqualTo(
            new Order(394L, "1234567890", "Book Name", 9.90, 1, OrderStatus.ACCEPTED, instant,
                instant, 21));

  }
}
