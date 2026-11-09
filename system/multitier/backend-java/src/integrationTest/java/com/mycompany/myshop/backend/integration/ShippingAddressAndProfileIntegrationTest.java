package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.core.entities.Address;
import com.mycompany.myshop.backend.core.entities.CustomerProfile;
import com.mycompany.myshop.backend.core.entities.Order;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.core.repositories.CustomerProfileRepository;
import com.mycompany.myshop.backend.core.repositories.OrderRepository;
import com.mycompany.myshop.backend.core.services.PageSpec;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** orders.shipping_address (JSONB), the owner_name customer filter, and customer_profiles, against real Postgres. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ShippingAddressAndProfileIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @BeforeEach
    void clear() {
        orderRepository.deleteAll();
        customerProfileRepository.deleteAll();
    }

    @Test
    void shippingAddressRoundTripsThroughTheJsonColumn() {
        var withAddress = order("ORD-001", "customer1");
        withAddress.setShippingAddress(new Address("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE"));
        orderRepository.save(withAddress);
        orderRepository.save(order("ORD-002", "customer1"));
        // Read back from the database, not the persistence context.
        entityManager.flush();
        entityManager.clear();

        assertThat(orderRepository.findByOrderNumber("ORD-001").orElseThrow().getShippingAddress())
                .isEqualTo(new Address("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE"));
        assertThat(orderRepository.findByOrderNumber("ORD-002").orElseThrow().getShippingAddress()).isNull();
    }

    @Test
    void customerFilterMatchesTheOwnerNameExactly() {
        orderRepository.save(order("ORD-A1", "customer1"));
        orderRepository.save(order("ORD-A2", "customer1"));
        orderRepository.save(order("ORD-B1", "customer2"));
        var pageable = PageSpec.of(null, null).toPageable();

        assertThat(orderRepository.countByOwnerName("customer1")).isEqualTo(2);
        assertThat(orderRepository.findPageByOwnerName("customer1", pageable))
                .extracting(Order::getOrderNumber).containsExactlyInAnyOrder("ORD-A1", "ORD-A2");
        assertThat(orderRepository.countByOwnerName("customer")).isZero();
        assertThat(orderRepository.countByOwnerNameAndOrderNumberContaining("customer1", "b1")).isZero();
        assertThat(orderRepository.findPageByOwnerNameAndOrderNumberContaining("customer2", "b1", pageable))
                .extracting(Order::getOrderNumber).containsExactly("ORD-B1");
    }

    @Test
    void profileIsStoredAndFoundByOwner() {
        var profile = new CustomerProfile("customer-sub");
        profile.setFirstName("Ana");
        profile.setLastName("Novak");
        profile.setStreet("Main St 1");
        profile.setPostalCode("10115");
        profile.setCity("Berlin");
        profile.setCountry("DE");
        profile.setUpdatedAt(Instant.parse("2026-11-09T12:00:00Z"));
        customerProfileRepository.save(profile);

        var found = customerProfileRepository.findByOwner("customer-sub").orElseThrow();
        assertThat(found.getCity()).isEqualTo("Berlin");
        assertThat(customerProfileRepository.findByOwner("other-sub")).isEmpty();
    }

    private static Order order(String orderNumber, String ownerName) {
        return new Order(orderNumber, Instant.parse("2026-01-01T00:00:00Z"), "US", "BOOK-123", 1,
                new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("0.0000"), new BigDecimal("0.00"),
                new BigDecimal("10.00"), new BigDecimal("0.1000"), new BigDecimal("1.00"), new BigDecimal("11.00"),
                OrderStatus.PLACED, null, ownerName + "-sub", ownerName);
    }
}
