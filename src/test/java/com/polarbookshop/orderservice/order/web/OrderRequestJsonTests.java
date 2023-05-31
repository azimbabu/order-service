package com.polarbookshop.orderservice.order.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
public class OrderRequestJsonTests {

  @Autowired
  private JacksonTester<OrderRequest> json;

  @Test
  void testSerialize() throws IOException {
    var orderRequest = new OrderRequest("1234567890", 1);
    var jsonContent = json.write(orderRequest);
    System.out.println(jsonContent.getJson());
    assertThat(jsonContent).extractingJsonPathStringValue("@.isbn").isEqualTo(orderRequest.isbn());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.quantity")
        .isEqualTo(orderRequest.quantity().intValue());
  }

  @Test
  void testDeserialize() throws IOException {
    var jsonContent = """
        {
          "isbn": "1234567890",
          "quantity": 1
        }
        """;
    assertThat(json.parse(jsonContent))
        .usingRecursiveComparison()
        .isEqualTo(new OrderRequest("1234567890", 1));
  }
}
