package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.infrastructure.persistence.entities.CouponJpaEntity;
import com.mycompany.myshop.backend.infrastructure.persistence.entities.OrderJpaEntity;
import com.mycompany.myshop.backend.infrastructure.persistence.queries.JpaCouponQuery;
import com.mycompany.myshop.backend.infrastructure.persistence.queries.JpaOrderQuery;
import com.mycompany.myshop.backend.usecases.queries.CouponListItem;
import com.mycompany.myshop.backend.usecases.queries.CouponQuery;
import com.mycompany.myshop.backend.usecases.queries.OrderListItem;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;
import com.mycompany.myshop.backend.usecases.queries.PageSpec;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// The paging in JpaOrderQuery and JpaCouponQuery is native SQL -- LIMIT/OFFSET plus a COUNT over the
// same predicate -- and neither the offset arithmetic nor the agreement between the two statements
// is something the compiler can check. So this test drives real Postgres.
//
// What it pins is the property that makes numbered pages correct rather than merely bounded:
// walking pages 1..totalPages must visit every row exactly once, and the total the client counts its
// buttons with must be the total of rows the filter actually matches. An off-by-one in the offset
// skips or repeats a row at a page boundary, and a count that disagrees with the filter draws
// buttons for pages that come back empty -- nothing else in the suite would notice either.
//
// @DataJpaTest for the reason OrderRepositoryIntegrationTest gives: the subject is one adapter, and
// it is transactional, so each test's rows roll back. The query classes are plain @Component beans,
// which @DataJpaTest does not scan, so they are imported explicitly.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, JpaOrderQuery.class, JpaCouponQuery.class})
class OffsetPagingIntegrationTest {

    // Every order this test writes carries this in its number, so the query's own filter scopes the
    // assertions to them. Rows are never truncated between tests anywhere in this suite.
    private static final String SCOPE = "OFFSET";

    private static final Instant BASE = Instant.parse("2026-03-10T12:00:00Z");

    @Autowired
    private OrderQuery orderQuery;

    @Autowired
    private CouponQuery couponQuery;

    @Autowired
    private EntityManager entityManager;

    @Test
    void walksEveryOrderExactlyOnceNewestFirst() {
        for (var i = 1; i <= 5; i++) {
            persistOrder(SCOPE + "-" + i, BASE.plusSeconds(i));
        }
        entityManager.flush();

        var visited = walkOrders(2);

        assertThat(visited).containsExactly(
            SCOPE + "-5", SCOPE + "-4", SCOPE + "-3", SCOPE + "-2", SCOPE + "-1");
    }

    // The case the tiebreaker exists for. order_timestamp is not unique, so an ORDER BY on the
    // timestamp alone leaves the order of the tied rows up to the plan -- and under OFFSET a row
    // that moves between two executions is a row served twice on one page and never on the next.
    @Test
    void doesNotSkipOrRepeatWhenTimestampsCollide() {
        persistOrder(SCOPE + "-A", BASE);
        persistOrder(SCOPE + "-B", BASE);
        persistOrder(SCOPE + "-C", BASE);
        entityManager.flush();

        var visited = walkOrders(1);

        assertThat(visited).containsExactly(SCOPE + "-C", SCOPE + "-B", SCOPE + "-A");
    }

    // The total is what the page buttons are drawn from, so it has to count the rows the filter
    // matches -- not the rows on this page, and not every order in the table.
    @Test
    void countsTheRowsTheFilterMatchesRatherThanThePage() {
        for (var i = 1; i <= 5; i++) {
            persistOrder(SCOPE + "-" + i, BASE.plusSeconds(i));
        }
        entityManager.flush();

        var page = orderQuery.listOrders(SCOPE, new PageSpec(1, 2));

        assertThat(page.items()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    // The last page is a partial one, which is where ceiling division earns its keep: five rows at
    // two per page is three pages, not two.
    @Test
    void servesAPartialLastPage() {
        for (var i = 1; i <= 5; i++) {
            persistOrder(SCOPE + "-" + i, BASE.plusSeconds(i));
        }
        entityManager.flush();

        var page = orderQuery.listOrders(SCOPE, new PageSpec(3, 2));

        assertThat(page.items()).hasSize(1);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    // A page past the end is an empty list rather than an error: a client that walks off the end
    // should see the end. The total still says how many pages there really were.
    @Test
    void returnsAnEmptyPagePastTheEnd() {
        persistOrder(SCOPE + "-1", BASE);
        entityManager.flush();

        var page = orderQuery.listOrders(SCOPE, new PageSpec(99, 10));

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void walksEveryCouponExactlyOnceNewestPublishedFirst() {
        persistCoupon(SCOPE + "-CPN-1");
        persistCoupon(SCOPE + "-CPN-2");
        persistCoupon(SCOPE + "-CPN-3");
        entityManager.flush();

        var visited = new ArrayList<String>();
        var first = couponQuery.listCoupons(new PageSpec(PageSpec.FIRST_PAGE, 2));
        visited.addAll(codes(first.items()));
        for (var pageNumber = PageSpec.FIRST_PAGE + 1; pageNumber <= first.totalPages(); pageNumber++) {
            visited.addAll(codes(couponQuery.listCoupons(new PageSpec(pageNumber, 2)).items()));
        }

        // Newest published first, so the coupons this test wrote lead the list in reverse insertion
        // order. Anything the container already held sorts after them.
        assertThat(visited).startsWith(SCOPE + "-CPN-3", SCOPE + "-CPN-2", SCOPE + "-CPN-1");
        assertThat(visited).hasSize((int) first.totalElements());
    }

    private List<String> walkOrders(int size) {
        var visited = new ArrayList<String>();
        var first = orderQuery.listOrders(SCOPE, new PageSpec(PageSpec.FIRST_PAGE, size));
        visited.addAll(orderNumbers(first.items()));
        for (var pageNumber = PageSpec.FIRST_PAGE + 1; pageNumber <= first.totalPages(); pageNumber++) {
            visited.addAll(orderNumbers(orderQuery.listOrders(SCOPE, new PageSpec(pageNumber, size)).items()));
        }
        return visited;
    }

    private static List<String> orderNumbers(List<OrderListItem> items) {
        return items.stream().map(OrderListItem::orderNumber).toList();
    }

    private static List<String> codes(List<CouponListItem> items) {
        return items.stream().map(CouponListItem::code).toList();
    }

    private void persistOrder(String orderNumber, Instant orderTimestamp) {
        var order = new OrderJpaEntity();
        order.setOrderNumber(orderNumber);
        order.setOrderTimestamp(orderTimestamp);
        order.setCountry("US");
        order.setSku("BOOK-123");
        order.setQuantity(2);
        order.setUnitPrice(new BigDecimal("10.00"));
        order.setBasePrice(new BigDecimal("20.00"));
        order.setDiscountRate(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setSubtotalPrice(new BigDecimal("20.00"));
        order.setTaxRate(new BigDecimal("0.1000"));
        order.setTaxAmount(new BigDecimal("2.00"));
        order.setTotalPrice(new BigDecimal("22.00"));
        order.setStatus(OrderStatus.PLACED);
        entityManager.persist(order);
    }

    private void persistCoupon(String code) {
        var coupon = new CouponJpaEntity();
        coupon.setCode(code);
        coupon.setDiscountRate(new BigDecimal("0.1000"));
        coupon.setValidFrom(BASE);
        coupon.setValidTo(BASE.plusSeconds(86_400));
        coupon.setUsageLimit(10);
        coupon.setUsedCount(0);
        entityManager.persist(coupon);
    }
}
