package com.mycompany.myshop.backend.infrastructure.persistence.queries;

import com.mycompany.myshop.backend.usecases.queries.CouponEffectiveness;
import com.mycompany.myshop.backend.usecases.queries.RevenueByCountryMonth;
import com.mycompany.myshop.backend.usecases.queries.SalesReportQuery;
import com.mycompany.myshop.backend.usecases.queries.TopSkuByRevenue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * The report, in SQL. Three statements, no entity, no loop: the aggregation happens where the rows
 * already are.
 *
 * <p>Native rather than JPQL, and deliberately so. {@code date_trunc} is Postgres-specific, and
 * that is fine here in a way it would never be one layer up: an adapter is the layer whose job is to
 * know which database it is talking to. Decoupling the application from the database's dialect is
 * not the same as refusing the database's set operations -- confusing the two is what pushed this
 * work into memory in the first place.
 *
 * <p>{@code CANCELLED} appears as a string because {@code status} is stored as the enum name and
 * native SQL never sees the enum. It is spelled once, as a constant.
 */
@Component
public class JpaSalesReportQuery implements SalesReportQuery {

    private static final String NOT_CANCELLED = "CANCELLED";
    private static final String STATUS = "status";
    private static final String LIMIT = "limit";

    // date_trunc collapses the timestamp to the first instant of its month, so the GROUP BY has a
    // month to group on without a month column existing.
    private static final String REVENUE_SQL = """
            SELECT o.country,
                   date_trunc('month', o.order_timestamp) AS month,
                   COUNT(*)              AS order_count,
                   SUM(o.quantity)       AS quantity,
                   SUM(o.subtotal_price) AS subtotal_price,
                   SUM(o.tax_amount)     AS tax_amount,
                   SUM(o.total_price)    AS total_price
            FROM orders o
            WHERE o.status <> :status
            GROUP BY o.country, month
            ORDER BY month DESC, o.country
            """;

    // The LIMIT is the point: the database stops after n rows, instead of the caller receiving every
    // SKU and discarding the tail. A plain int, not a page vocabulary -- this report is not paged.
    private static final String TOP_SKU_SQL = """
            SELECT o.sku,
                   COUNT(*)           AS order_count,
                   SUM(o.quantity)    AS quantity,
                   SUM(o.total_price) AS total_price
            FROM orders o
            WHERE o.status <> :status
            GROUP BY o.sku
            ORDER BY SUM(o.total_price) DESC, o.sku
            LIMIT :limit
            """;

    // LEFT JOIN so a coupon nobody used still gets a row, with zeroes rather than an absence. The
    // status predicate sits in the JOIN condition, not in WHERE: in WHERE it would discard the
    // coupon whose only orders were cancelled, turning the left join back into an inner one.
    private static final String COUPON_EFFECTIVENESS_SQL = """
            SELECT c.code,
                   c.usage_limit,
                   c.used_count,
                   COUNT(o.id)                          AS order_count,
                   COALESCE(SUM(o.discount_amount), 0)  AS discount_amount
            FROM coupons c
            LEFT JOIN orders o
                   ON o.applied_coupon_code = c.code
                  AND o.status <> :status
            GROUP BY c.code, c.usage_limit, c.used_count
            ORDER BY COALESCE(SUM(o.discount_amount), 0) DESC, c.code
            """;

    private final EntityManager entityManager;

    public JpaSalesReportQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<RevenueByCountryMonth> revenueByCountryAndMonth() {
        var query = entityManager.createNativeQuery(REVENUE_SQL, Object[].class)
                .setParameter(STATUS, NOT_CANCELLED);

        return rows(query).stream()
                .map(JpaSalesReportQuery::toRevenue)
                .toList();
    }

    @Override
    public List<TopSkuByRevenue> topSkusByRevenue(int limit) {
        var query = entityManager.createNativeQuery(TOP_SKU_SQL, Object[].class)
                .setParameter(STATUS, NOT_CANCELLED)
                .setParameter(LIMIT, limit);

        return rows(query).stream()
                .map(JpaSalesReportQuery::toTopSku)
                .toList();
    }

    @Override
    public List<CouponEffectiveness> couponEffectiveness() {
        var query = entityManager.createNativeQuery(COUPON_EFFECTIVENESS_SQL, Object[].class)
                .setParameter(STATUS, NOT_CANCELLED);

        return rows(query).stream()
                .map(JpaSalesReportQuery::toCouponEffectiveness)
                .toList();
    }

    // createNativeQuery's JPA signature is raw whatever result class it is handed, so the cast is
    // unavoidable; it is confined to this one method rather than repeated three times.
    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(Query query) {
        return query.getResultList();
    }

    private static RevenueByCountryMonth toRevenue(Object[] row) {
        return new RevenueByCountryMonth(
                (String) row[0],
                instant(row[1]),
                count(row[2]),
                count(row[3]),
                decimal(row[4]),
                decimal(row[5]),
                decimal(row[6]));
    }

    private static TopSkuByRevenue toTopSku(Object[] row) {
        return new TopSkuByRevenue(
                (String) row[0],
                count(row[1]),
                count(row[2]),
                decimal(row[3]));
    }

    private static CouponEffectiveness toCouponEffectiveness(Object[] row) {
        return new CouponEffectiveness(
                (String) row[0],
                integer(row[1]),
                integer(row[2]),
                count(row[3]),
                decimal(row[4]));
    }

    // A native result is typed by the driver rather than by a mapping, so these three narrow whatever
    // the column came back as. Cheap insurance, and the only place in the read side that needs it.

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

    private static long count(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static Integer integer(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal asDecimal ? asDecimal : new BigDecimal(value.toString());
    }
}
