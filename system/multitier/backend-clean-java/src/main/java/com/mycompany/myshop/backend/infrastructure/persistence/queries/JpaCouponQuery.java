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

/**
 * See {@link JpaOrderQuery} for why the read side looks like this. The one thing specific to coupons
 * is the key it sorts and pages on: the table has no timestamp column, so "newest published first"
 * has to come from the surrogate {@code id} -- which is exactly why the sort lives here and not in
 * the port. The id never leaves infrastructure, and the domain's rule that a {@code Coupon} has no
 * {@code Long id} still holds.
 */
@Component
public class JpaCouponQuery implements CouponQuery {

    private static final String LIST_SELECT = """
            SELECT c.code, c.discount_rate, c.valid_from, c.valid_to, c.usage_limit, c.used_count
            FROM coupons c
            """;

    // The cursor a client holds is a code, but the sort key is the id, so the code has to be
    // resolved back to an id before it can be compared. That subquery is what it costs to keep the
    // surrogate key inside this package, and it is cheap: `code` is UNIQUE, so it is one index
    // lookup. An unknown code makes the subquery NULL and the comparison unknown, so the page comes
    // back empty rather than silently restarting from the top -- which is the right answer to a
    // cursor that names a coupon that no longer exists.
    private static final String KEYSET_PREDICATE =
            "WHERE c.id < (SELECT previous.id FROM coupons previous WHERE previous.code = :cursorCode)\n";

    // LIMIT is bound to size + 1 for the same reason as in JpaOrderQuery: the row that does not fit
    // is how hasMore is answered.
    private static final String LIST_ORDER = """
            ORDER BY c.id DESC
            LIMIT :limit
            """;

    private static final String CURSOR_CODE = "cursorCode";
    private static final String LIMIT = "limit";

    private final EntityManager entityManager;

    public JpaCouponQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<CouponListItem> listCoupons(PageSpec<String> page) {
        var cursor = page.cursor();
        var keyset = cursor != null && !cursor.isBlank();

        var query = entityManager.createNativeQuery(
                        LIST_SELECT + (keyset ? KEYSET_PREDICATE : "") + LIST_ORDER, Object[].class)
                .setParameter(LIMIT, page.size() + 1);
        if (keyset) {
            query.setParameter(CURSOR_CODE, cursor);
        }

        var rows = rows(query);
        var hasMore = rows.size() > page.size();
        var onThisPage = hasMore ? rows.subList(0, page.size()) : rows;

        return new Page<>(onThisPage.stream().map(JpaCouponQuery::toListItem).toList(), hasMore);
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
