package com.mycompany.myshop.backend.component.latest;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

// GET /api/admin/customers: the usernames that have placed an order, distinct, A to Z, as a bare
// JSON array. The domain Order has no customer (this variant has no authentication), so the
// customer is written to the shared schema's owner_name column directly.
class BrowseCustomersComponentTest extends BaseComponentTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void ordersWithoutACustomerAreNotListed() {
        orders.add(order("ORD-1"));

        var response = restTemplate.getForEntity("/api/admin/customers", String[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void listsEachCustomerOnceFromAToZ() {
        orderFor("ORD-1", "carol");
        orderFor("ORD-2", "alice");
        orderFor("ORD-3", "carol");

        var response = restTemplate.getForEntity("/api/admin/customers", String[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("alice", "carol");
    }

    private void orderFor(String orderNumber, String customer) {
        orders.add(order(orderNumber));
        jdbc.update("UPDATE orders SET owner_name = ? WHERE order_number = ?", customer, orderNumber);
    }

    private static Order order(String orderNumber) {
        var pricing = new OrderPricing(
            Money.of("10.00"), 2, Money.of("20.00"),
            Rate.ZERO, Money.ZERO, Money.of("20.00"),
            Rate.of("0.10"), Money.of("2.00"), Money.of("22.00"));

        return Order.restore(
            OrderNumber.of(orderNumber), Instant.parse("2026-03-10T12:00:00Z"), Country.of("US"),
            Sku.of("BOOK-123"), pricing, OrderStatus.PLACED, null);
    }
}
