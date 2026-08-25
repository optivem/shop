package com.mycompany.myshop.backend.infrastructure.persistence.queries;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.usecases.queries.OrderDetail;
import com.mycompany.myshop.backend.usecases.queries.OrderListItem;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;
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
import java.util.Optional;

// The read side, in SQL. The SELECT list is the projection: these queries fetch the columns the
// response holds and nothing else, and no OrderJpaEntity is ever materialised -- so nothing enters
// the persistence context, nothing is dirty-checked, and no domain constructor runs.
//
// The list query is native rather than JPQL, for the same reason JpaSalesReportQuery is: JPQL has
// no OFFSET of its own, only setFirstResult on the query object, and an adapter is the layer whose
// job it is to know which database it is talking to. The detail query stays JPQL -- one predicate
// on a unique column has nothing to gain.
//
// Rows come back as Object[] rather than through a SELECT new ... constructor expression because the
// projection records are not allowed to name a domain type: status leaves this class as a plain
// string. The mapping is one place, directly under the SELECT list it mirrors.
@Component
public class JpaOrderQuery implements OrderQuery {

    private static final String LIST_SELECT = """
            SELECT o.order_number, o.order_timestamp, o.sku, o.country, o.quantity,
                   o.total_price, o.status, o.applied_coupon_code
            FROM orders o
            """;

    private static final String COUNT_SELECT = """
            SELECT COUNT(*)
            FROM orders o
            """;

    private static final String FILTER_PREDICATE =
            "LOWER(o.order_number) LIKE LOWER('%' || :orderNumber || '%')";

    // order_number is the tiebreaker rather than the surrogate id, because the id is not allowed out
    // of this package -- and without a tiebreaker the sort is not total: two orders sharing an
    // instant could come back in either order, which under OFFSET means one of them appears on two
    // pages and the other on none.
    //
    // OFFSET is what page numbers cost. Postgres still reads and discards every row before the
    // window, so page 200 is slower than page 1 and the difference grows with the page number.
    // idx_orders_recent keeps it an index scan rather than a sort of the whole table, which is what
    // makes the cost linear rather than ruinous; it does not make it constant. That is the trade
    // accepted in exchange for "page 3 of 26" and a row of numbered buttons, neither of which a
    // resume-token scheme can offer.
    private static final String LIST_ORDER = """
            ORDER BY o.order_timestamp DESC, o.order_number DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final String DETAIL_SELECT = """
            SELECT o.orderNumber, o.orderTimestamp, o.sku, o.quantity, o.unitPrice,
                   o.basePrice, o.discountRate, o.discountAmount, o.subtotalPrice,
                   o.taxRate, o.taxAmount, o.totalPrice, o.status, o.country,
                   o.appliedCouponCode
            FROM OrderJpaEntity o
            WHERE o.orderNumber = :orderNumber
            """;

    private static final String ORDER_NUMBER = "orderNumber";
    private static final String LIMIT = "limit";
    private static final String OFFSET = "offset";

    private final EntityManager entityManager;

    public JpaOrderQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // Two statements, not one: the page of rows, and the count of all the rows it was taken from.
    // The count is what pays for the page numbers, and it is deliberately run against the same WHERE
    // as the SELECT -- a total that ignored the filter would number pages that the filtered list
    // does not have.
    @Override
    public Page<OrderListItem> listOrders(String orderNumberFilter, PageSpec page) {
        var filter = orderNumberFilter == null ? null : orderNumberFilter.trim();
        var filtered = filter != null && !filter.isEmpty();
        var where = filtered ? "WHERE " + FILTER_PREDICATE + "\n" : "";

        var query = entityManager.createNativeQuery(LIST_SELECT + where + LIST_ORDER, Object[].class)
                .setParameter(LIMIT, page.size())
                .setParameter(OFFSET, page.offset());
        var countQuery = entityManager.createNativeQuery(COUNT_SELECT + where);
        if (filtered) {
            query.setParameter(ORDER_NUMBER, filter);
            countQuery.setParameter(ORDER_NUMBER, filter);
        }

        var items = rows(query).stream().map(JpaOrderQuery::toListItem).toList();
        var totalElements = ((Number) countQuery.getSingleResult()).longValue();

        return new Page<>(items, page.page(), page.size(), totalElements);
    }

    @Override
    public Optional<OrderDetail> findOrderDetail(String orderNumber) {
        return entityManager.createQuery(DETAIL_SELECT, Object[].class)
                .setParameter(ORDER_NUMBER, orderNumber)
                .getResultList().stream()
                .findFirst()
                .map(JpaOrderQuery::toDetail);
    }

    // createNativeQuery's JPA signature is raw whatever result class it is handed, so the cast is
    // unavoidable; it is confined to this one method.
    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(Query query) {
        return query.getResultList();
    }

    private static OrderListItem toListItem(Object[] row) {
        return new OrderListItem(
                (String) row[0],
                instant(row[1]),
                (String) row[2],
                (String) row[3],
                (Integer) row[4],
                (BigDecimal) row[5],
                (String) row[6],
                (String) row[7]);
    }

    private static OrderDetail toDetail(Object[] row) {
        return new OrderDetail(
                (String) row[0],
                (Instant) row[1],
                (String) row[2],
                (Integer) row[3],
                (BigDecimal) row[4],
                (BigDecimal) row[5],
                (BigDecimal) row[6],
                (BigDecimal) row[7],
                (BigDecimal) row[8],
                (BigDecimal) row[9],
                (BigDecimal) row[10],
                (BigDecimal) row[11],
                name(row[12]),
                (String) row[13],
                (String) row[14]);
    }

    // The list is native, so its timestamp is typed by the driver rather than by a mapping. The
    // detail query is JPQL and hands back an Instant already, which is why only one of the two
    // mappers needs this.
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

    // Only the JPQL path sees the enum; native SQL reads the stored string straight out of the
    // column.
    private static String name(Object status) {
        return status == null ? null : ((OrderStatus) status).name();
    }
}
