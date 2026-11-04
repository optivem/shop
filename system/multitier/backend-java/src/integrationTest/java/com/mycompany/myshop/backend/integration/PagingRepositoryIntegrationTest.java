package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.core.entities.Coupon;
import com.mycompany.myshop.backend.core.entities.Order;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.core.repositories.CouponRepository;
import com.mycompany.myshop.backend.core.repositories.OrderRepository;
import com.mycompany.myshop.backend.core.services.PageSpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// The paged list queries against real Postgres: LIMIT/OFFSET, the COUNT twins, and a sort that stays
// stable when timestamps tie.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PagingRepositoryIntegrationTest {

    private static final Instant SAME_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        couponRepository.deleteAll();
    }

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
}
