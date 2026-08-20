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
import com.mycompany.myshop.backend.usecases.queries.OrderCursor;
import com.mycompany.myshop.backend.usecases.queries.OrderListItem;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;
import com.mycompany.myshop.backend.usecases.queries.Page;
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

// The keyset pagination in JpaOrderQuery and JpaCouponQuery is native SQL: a row-value comparison in
// one, a code-to-id subquery in the other. Neither is something the compiler can check and neither
// behaves the same on an in-memory database, so this test drives real Postgres.
//
// What it pins is the property that makes paging correct rather than merely bounded: walking the
// pages must visit every row exactly once. A cursor that is off by one row skips or repeats at the
// page boundary and nothing else in the suite would notice -- the pages would still be the right
// size and still be in the right order.
//
// @DataJpaTest for the reason OrderRepositoryIntegrationTest gives: the subject is one adapter, and
// it is transactional, so each test's rows roll back. The query classes are plain @Component beans,
// which @DataJpaTest does not scan, so they are imported explicitly.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, JpaOrderQuery.class, JpaCouponQuery.class})
class KeysetPagingIntegrationTest {

    // Every order this test writes carries this in its number, so the query's own filter scopes the
    // assertions to them. Rows are never truncated between tests anywhere in this suite.
    private static final String SCOPE = "KEYSET";

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

    // The case the tiebreaker exists for. order_timestamp is not unique, so a cursor holding only
    // the timestamp would either re-read the row it just returned or step over its neighbour.
    @Test
    void doesNotSkipOrRepeatWhenTimestampsCollide() {
        persistOrder(SCOPE + "-A", BASE);
        persistOrder(SCOPE + "-B", BASE);
        persistOrder(SCOPE + "-C", BASE);
        entityManager.flush();

        var visited = walkOrders(1);

        assertThat(visited).containsExactly(SCOPE + "-C", SCOPE + "-B", SCOPE + "-A");
    }

    @Test
    void reportsHasMoreOnlyWhileRowsRemain() {
        persistOrder(SCOPE + "-1", BASE);
        persistOrder(SCOPE + "-2", BASE.plusSeconds(1));
        entityManager.flush();

        var first = orderQuery.listOrders(SCOPE, new PageSpec<>(1, null));
        assertThat(first.hasMore()).isTrue();

        var second = orderQuery.listOrders(SCOPE, cursorAfter(first, 1));
        assertThat(second.hasMore()).isFalse();
        assertThat(second.items()).hasSize(1);
    }

    @Test
    void walksEveryCouponExactlyOnceNewestPublishedFirst() {
        persistCoupon(SCOPE + "-CPN-1");
        persistCoupon(SCOPE + "-CPN-2");
        persistCoupon(SCOPE + "-CPN-3");
        entityManager.flush();

        var visited = new ArrayList<String>();
        var page = couponQuery.listCoupons(new PageSpec<>(2, null));
        visited.addAll(page.items().stream().map(CouponListItem::code).toList());
        while (page.hasMore()) {
            page = couponQuery.listCoupons(
                new PageSpec<>(2, page.last().map(CouponListItem::code).orElseThrow()));
            visited.addAll(page.items().stream().map(CouponListItem::code).toList());
        }

        // Newest published first, so the coupons this test wrote lead the list in reverse insertion
        // order. Anything the container already held sorts after them.
        assertThat(visited).startsWith(SCOPE + "-CPN-3", SCOPE + "-CPN-2", SCOPE + "-CPN-1");
    }

    // A cursor naming a coupon that no longer exists makes the subquery NULL, and a comparison
    // against NULL is unknown rather than true -- so the page is empty instead of silently
    // restarting from the newest coupon, which would loop a paging client forever.
    @Test
    void returnsNothingForACouponCursorThatNoLongerExists() {
        persistCoupon(SCOPE + "-CPN-1");
        entityManager.flush();

        var page = couponQuery.listCoupons(new PageSpec<>(10, SCOPE + "-CPN-GONE"));

        assertThat(page.items()).isEmpty();
        assertThat(page.hasMore()).isFalse();
    }

    private List<String> walkOrders(int size) {
        var visited = new ArrayList<String>();
        var page = orderQuery.listOrders(SCOPE, new PageSpec<>(size, null));
        visited.addAll(page.items().stream().map(OrderListItem::orderNumber).toList());
        while (page.hasMore()) {
            page = orderQuery.listOrders(SCOPE, cursorAfter(page, size));
            visited.addAll(page.items().stream().map(OrderListItem::orderNumber).toList());
        }
        return visited;
    }

    private static PageSpec<OrderCursor> cursorAfter(Page<OrderListItem> page, int size) {
        return new PageSpec<>(size, page.last().map(OrderCursor::after).orElseThrow());
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
