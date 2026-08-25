package com.mycompany.myshop.backend.infrastructure.persistence.queries;

import com.mycompany.myshop.backend.usecases.queries.CouponListItem;
import com.mycompany.myshop.backend.usecases.queries.CouponQuery;
import com.mycompany.myshop.backend.usecases.queries.Page;
import com.mycompany.myshop.backend.usecases.queries.PageSpec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

// See JpaOrderQuery for why the read side looks like this. The one thing specific to coupons
// is the key it sorts on: the table has no timestamp column, so "newest published first"
// has to come from the surrogate id -- which is exactly why the sort lives here and not in
// the port. The id never leaves infrastructure, and the domain's rule that a Coupon has no
// Long id still holds.
@Component
public class JpaCouponQuery implements CouponQuery {

    private static final String LIST_SELECT = """
            SELECT c.code, c.discount_rate, c.valid_from, c.valid_to, c.usage_limit, c.used_count
            FROM coupons c
            """;

    private static final String COUNT_SELECT = """
            SELECT COUNT(*)
            FROM coupons c
            """;

    // See JpaOrderQuery#LIST_ORDER for what OFFSET costs. Coupons are the list where it costs least:
    // there are few of them, and unlike orders they are not appended to while an admin reads them,
    // so the page boundaries do not drift under the reader.
    private static final String LIST_ORDER = """
            ORDER BY c.id DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final String LIMIT = "limit";
    private static final String OFFSET = "offset";

    private final EntityManager entityManager;

    public JpaCouponQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<CouponListItem> listCoupons(PageSpec page) {
        var query = entityManager.createNativeQuery(LIST_SELECT + LIST_ORDER, Object[].class)
                .setParameter(LIMIT, page.size())
                .setParameter(OFFSET, page.offset());

        var items = rows(query).stream().map(JpaCouponQuery::toListItem).toList();
        var totalElements = ((Number) entityManager.createNativeQuery(COUNT_SELECT)
                .getSingleResult()).longValue();

        return new Page<>(items, page.page(), page.size(), totalElements);
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(Query query) {
        return query.getResultList();
    }

    private static CouponListItem toListItem(Object[] row) {
        return new CouponListItem(
                (String) row[0],
                (BigDecimal) row[1],
                instant(row[2]),
                instant(row[3]),
                (Integer) row[4],
                (Integer) row[5]);
    }

    // Native results are typed by the driver rather than by a mapping. See JpaOrderQuery#instant.
    private static Instant instant(Object value) {
        return switch (value) {
            case null -> null;
            case Instant asInstant -> asInstant;
            case OffsetDateTime asOffset -> asOffset.toInstant();
            case Timestamp asTimestamp -> asTimestamp.toInstant();
            default -> throw new IllegalStateException(
                    "Unexpected timestamp type from the driver: " + value.getClass());
        };
    }
}
