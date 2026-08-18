package com.mycompany.myshop.backend.infrastructure.persistence.queries;

import com.mycompany.myshop.backend.usecases.queries.CouponListItem;
import com.mycompany.myshop.backend.usecases.queries.CouponQuery;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * See {@link JpaOrderQuery} for why the read side looks like this. The one thing specific to coupons
 * is the ordering: the table has no timestamp column, so "newest published first" has to come from
 * the surrogate {@code id} -- which is exactly why the sort lives here and not in the port. The id
 * never leaves infrastructure, and the domain's rule that a {@code Coupon} has no {@code Long id}
 * still holds.
 */
@Component
public class JpaCouponQuery implements CouponQuery {

    private static final String LIST_SELECT = """
            SELECT c.code, c.discountRate, c.validFrom, c.validTo, c.usageLimit, c.usedCount
            FROM CouponJpaEntity c
            ORDER BY c.id DESC
            """;

    private final EntityManager entityManager;

    public JpaCouponQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<CouponListItem> listCoupons() {
        return entityManager.createQuery(LIST_SELECT, Object[].class)
                .getResultList().stream()
                .map(JpaCouponQuery::toListItem)
                .toList();
    }

    private static CouponListItem toListItem(Object[] row) {
        return new CouponListItem(
                (String) row[0],
                (BigDecimal) row[1],
                (Instant) row[2],
                (Instant) row[3],
                (Integer) row[4],
                (Integer) row[5]);
    }
}
