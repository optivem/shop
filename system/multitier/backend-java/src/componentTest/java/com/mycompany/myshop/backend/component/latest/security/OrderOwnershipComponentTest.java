package com.mycompany.myshop.backend.component.latest.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.backendtest.configuration.TestAuthConfiguration;
import com.mycompany.myshop.backend.core.entities.Order;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Who may see and change which order: customers see only their own orders (another customer's order
 * is reported as non-existent), admins see and cancel every order, and an order with no owner is
 * visible to admins only.
 */
class OrderOwnershipComponentTest extends BaseComponentTest {

    private static final String CUSTOMER_SUBJECT = "customer-user";
    private static final String OWNED_ORDER = "ORD-OWNED";
    private static final String UNOWNED_ORDER = "ORD-UNOWNED";

    private final TestRestTemplate client = new TestRestTemplate();

    @BeforeEach
    void saveOrders() {
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        orderRepository.save(order(OWNED_ORDER, CUSTOMER_SUBJECT, "customer1"));
        orderRepository.save(order(UNOWNED_ORDER, null, null));
    }

    @Test
    void ownerCanViewOwnOrder() {
        assertThat(call(HttpMethod.GET, "/api/orders/" + OWNED_ORDER, TestAuthConfiguration.CUSTOMER_TOKEN).getStatusCode())
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void anotherCustomerCannotViewOrder() {
        var response = call(HttpMethod.GET, "/api/orders/" + OWNED_ORDER, TestAuthConfiguration.OTHER_CUSTOMER_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminCanViewAnyOrder() {
        assertThat(call(HttpMethod.GET, "/api/orders/" + OWNED_ORDER, TestAuthConfiguration.ADMIN_TOKEN).getStatusCode())
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void customerCannotViewOrderWithoutOwner() {
        assertThat(call(HttpMethod.GET, "/api/orders/" + UNOWNED_ORDER, TestAuthConfiguration.CUSTOMER_TOKEN).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void customerHistoryListsOnlyOwnOrders() {
        var body = call(HttpMethod.GET, "/api/orders", TestAuthConfiguration.CUSTOMER_TOKEN).getBody();

        assertThat(body).contains(OWNED_ORDER).doesNotContain(UNOWNED_ORDER);
    }

    @Test
    void anotherCustomerHistoryDoesNotListOrder() {
        var body = call(HttpMethod.GET, "/api/orders", TestAuthConfiguration.OTHER_CUSTOMER_TOKEN).getBody();

        assertThat(body).doesNotContain(OWNED_ORDER).doesNotContain(UNOWNED_ORDER);
    }

    @Test
    void adminHistoryListsAllOrdersWithCustomer() {
        var body = call(HttpMethod.GET, "/api/orders", TestAuthConfiguration.ADMIN_TOKEN).getBody();

        assertThat(body).contains(OWNED_ORDER).contains(UNOWNED_ORDER).contains("\"customer\":\"customer1\"");
    }

    @Test
    void anotherCustomerCannotCancelOrder() {
        var response = call(HttpMethod.POST, "/api/orders/" + OWNED_ORDER + "/cancel", TestAuthConfiguration.OTHER_CUSTOMER_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(orderRepository.findByOrderNumber(OWNED_ORDER).orElseThrow().getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void ownerCanCancelOwnOrder() {
        var response = call(HttpMethod.POST, "/api/orders/" + OWNED_ORDER + "/cancel", TestAuthConfiguration.CUSTOMER_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void adminCanCancelAnyOrder() {
        var response = call(HttpMethod.POST, "/api/orders/" + OWNED_ORDER + "/cancel", TestAuthConfiguration.ADMIN_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void adminCannotPlaceOrder() {
        var response = call(HttpMethod.POST, "/api/orders", TestAuthConfiguration.ADMIN_TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<String> call(HttpMethod method, String path, String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return client.exchange("http://localhost:" + port + path, method, new HttpEntity<>(headers), String.class);
    }

    private static Order order(String orderNumber, String owner, String ownerName) {
        return new Order(
            orderNumber, Instant.parse("2026-03-10T12:00:00Z"), "US",
            "BOOK-123", 2, new BigDecimal("10.00"), new BigDecimal("20.00"),
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00"),
            new BigDecimal("0.10"), new BigDecimal("2.00"), new BigDecimal("22.00"),
            OrderStatus.PLACED, null, owner, ownerName);
    }
}
