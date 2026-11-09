package com.mycompany.myshop.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.AbstractIntegrationTest;
import com.mycompany.myshop.core.entities.Address;
import com.mycompany.myshop.core.entities.Coupon;
import com.mycompany.myshop.core.entities.CustomerProfile;
import com.mycompany.myshop.core.entities.Order;
import com.mycompany.myshop.core.entities.OrderStatus;
import com.mycompany.myshop.core.repositories.CouponRepository;
import com.mycompany.myshop.core.repositories.CustomerProfileRepository;
import com.mycompany.myshop.core.repositories.OrderRepository;
import com.mycompany.myshop.core.services.PageSpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final Instant SAME_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @BeforeEach
    void clearOrders() {
        orderRepository.deleteAll();
        couponRepository.deleteAll();
        customerProfileRepository.deleteAll();
    }

    @Test
    void savesAndReadsBackOrder() {
        Order order = new Order(
            "ORD-001",
            Instant.parse("2026-01-01T00:00:00Z"),
            "US",
            "BOOK-123",
            2,
            new BigDecimal("10.00"),
            new BigDecimal("20.00"),
            new BigDecimal("0.0000"),
            new BigDecimal("0.00"),
            new BigDecimal("20.00"),
            new BigDecimal("0.1000"),
            new BigDecimal("2.00"),
            new BigDecimal("22.00"),
            OrderStatus.PLACED,
            null,
            "customer-sub",
            "customer1"
        );

        orderRepository.save(order);

        Optional<Order> found = orderRepository.findByOrderNumber("ORD-001");
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("BOOK-123");
        assertThat(found.get().getTotalPrice()).isEqualByComparingTo(new BigDecimal("22.00"));
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(found.get().getOwner()).isEqualTo("customer-sub");
        assertThat(found.get().getOwnerName()).isEqualTo("customer1");
    }

    // The paged list queries: LIMIT/OFFSET, the COUNT twins, and a sort that stays stable when
    // timestamps tie.

    @Test
    void pagesOfOrdersWithTiedTimestampsNeitherRepeatNorSkipRows() {
        for (int i = 1; i <= 5; i++) {
            orderRepository.save(order("ORD-00" + i, "customer-sub"));
        }

        var seen = new ArrayList<String>();
        for (int page = 1; page <= 3; page++) {
            orderRepository.findPage(new PageSpec(page, 2).toPageable()).forEach(o -> seen.add(o.getOrderNumber()));
        }

        // id DESC breaks the timestamp tie: newest insert first.
        assertThat(seen).containsExactly("ORD-005", "ORD-004", "ORD-003", "ORD-002", "ORD-001");
        assertThat(orderRepository.count()).isEqualTo(5);
    }

    @Test
    void pageOfOwnOrdersIsBoundedAndCountedOverTheSamePredicate() {
        for (int i = 1; i <= 3; i++) {
            orderRepository.save(order("ORD-A" + i, "customer-sub"));
        }
        orderRepository.save(order("ORD-B1", "other-sub"));

        List<Order> page = orderRepository.findPageByOwner("customer-sub", new PageSpec(1, 2).toPageable());

        assertThat(page).extracting(Order::getOrderNumber).containsExactly("ORD-A3", "ORD-A2");
        assertThat(orderRepository.countByOwner("customer-sub")).isEqualTo(3);
        assertThat(orderRepository.countByOwnerAndOrderNumberContaining("customer-sub", "a1")).isEqualTo(1);
        assertThat(orderRepository.findPageByOwnerAndOrderNumberContaining("customer-sub", "a1", new PageSpec(1, 50).toPageable()))
                .extracting(Order::getOrderNumber).containsExactly("ORD-A1");
        assertThat(orderRepository.countByOrderNumberContaining("ORD-")).isEqualTo(4);
    }

    @Test
    void pagePastTheLastRowIsEmpty() {
        orderRepository.save(order("ORD-001", "customer-sub"));

        assertThat(orderRepository.findPage(new PageSpec(2, 50).toPageable())).isEmpty();
    }

    @Test
    void couponsArePagedNewestFirst() {
        for (int i = 1; i <= 3; i++) {
            couponRepository.save(new Coupon("CODE" + i, new BigDecimal("0.1000"), null, null, 10, 0));
        }

        assertThat(couponRepository.findPage(new PageSpec(1, 2).toPageable())).extracting(Coupon::getCode).containsExactly("CODE3", "CODE2");
        assertThat(couponRepository.findPage(new PageSpec(2, 2).toPageable())).extracting(Coupon::getCode).containsExactly("CODE1");
    }

    private static Order order(String orderNumber, String owner) {
        return new Order(orderNumber, SAME_TIME, "US", "BOOK-123", 1,
                new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("0.0000"), new BigDecimal("0.00"),
                new BigDecimal("10.00"), new BigDecimal("0.1000"), new BigDecimal("1.00"), new BigDecimal("11.00"),
                OrderStatus.PLACED, null, owner, "user");
    }

    // Shipping address (JSONB), the admin customer filter on owner_name, and customer profiles.

    @Test
    void shippingAddressRoundTripsThroughTheJsonColumn() {
        var withAddress = ownedBy("ORD-001", "customer1");
        withAddress.setShippingAddress(new Address("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE"));
        orderRepository.save(withAddress);
        orderRepository.save(ownedBy("ORD-002", "customer1"));

        assertThat(orderRepository.findByOrderNumber("ORD-001").orElseThrow().getShippingAddress())
                .isEqualTo(new Address("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE"));
        assertThat(orderRepository.findByOrderNumber("ORD-002").orElseThrow().getShippingAddress()).isNull();
    }

    @Test
    void customerFilterMatchesTheOwnerNameExactly() {
        orderRepository.save(ownedBy("ORD-A1", "customer1"));
        orderRepository.save(ownedBy("ORD-A2", "customer1"));
        orderRepository.save(ownedBy("ORD-B1", "customer2"));
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

    /** An order placed by the named customer (owner_name), for the admin customer filter. */
    private static Order ownedBy(String orderNumber, String ownerName) {
        var order = order(orderNumber, ownerName + "-sub");
        order.setOwnerName(ownerName);
        return order;
    }
}
