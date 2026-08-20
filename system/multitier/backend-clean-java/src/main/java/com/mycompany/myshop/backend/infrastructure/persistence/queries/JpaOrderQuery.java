package com.mycompany.myshop.backend.infrastructure.persistence.queries;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.usecases.queries.OrderCursor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// The read side, in SQL. The SELECT list is the projection: these queries fetch the columns the
// response holds and nothing else, and no OrderJpaEntity is ever materialised -- so nothing enters
// the persistence context, nothing is dirty-checked, and no domain constructor runs.
//
// The list query is native rather than JPQL, for the same reason JpaSalesReportQuery is: its keyset
// predicate is a row-value comparison, and an adapter is the layer whose job it is to know which
// database it is talking to. The detail query stays JPQL -- one predicate on a unique column has
// nothing to gain.
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

    private static final String FILTER_PREDICATE =
            "LOWER(o.order_number) LIKE LOWER('%' || :orderNumber || '%')";

    // Row-value comparison, and not OFFSET. `(a, b) < (x, y)` is one comparison Postgres can satisfy
    // by descending idx_orders_recent straight to the resume point; `OFFSET 10000` makes it read ten
    // thousand rows in order to throw them away, and that cost grows with the page number while this
    // one does not. The columns and their order have to match the ORDER BY exactly or the index does
    // not apply -- which is why the tuple is spelled as a tuple rather than expanded into
    // `ts < :ts OR (ts = :ts AND num < :num)`, a form that means the same thing and plans worse.
    private static final String KEYSET_PREDICATE =
            "(o.order_timestamp, o.order_number) < (:cursorTimestamp, :cursorOrderNumber)";

    // order_number is the tiebreaker rather than the surrogate id, because the id is not allowed out
    // of this package. LIMIT is bound to size + 1: the row that does not fit on the page is how
    // hasMore gets answered without a second query.
    private static final String LIST_ORDER = """
            ORDER BY o.order_timestamp DESC, o.order_number DESC
            LIMIT :limit
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
    private static final String CURSOR_TIMESTAMP = "cursorTimestamp";
    private static final String CURSOR_ORDER_NUMBER = "cursorOrderNumber";
    private static final String LIMIT = "limit";

    private final EntityManager entityManager;

    public JpaOrderQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<OrderListItem> listOrders(String orderNumberFilter, PageSpec<OrderCursor> page) {
        var filter = orderNumberFilter == null ? null : orderNumberFilter.trim();
        var filtered = filter != null && !filter.isEmpty();
        var cursor = page.cursor();

        var predicates = new ArrayList<String>();
        if (filtered) {
            predicates.add(FILTER_PREDICATE);
        }
        if (cursor != null) {
            predicates.add(KEYSET_PREDICATE);
        }
        var where = predicates.isEmpty() ? "" : "WHERE " + String.join(" AND ", predicates) + "\n";

        var query = entityManager.createNativeQuery(LIST_SELECT + where + LIST_ORDER, Object[].class)
                .setParameter(LIMIT, page.size() + 1);
        if (filtered) {
            query.setParameter(ORDER_NUMBER, filter);
        }
        if (cursor != null) {
            query.setParameter(CURSOR_TIMESTAMP, cursor.orderTimestamp());
            query.setParameter(CURSOR_ORDER_NUMBER, cursor.orderNumber());
        }

        var rows = rows(query);
        var hasMore = rows.size() > page.size();
        var onThisPage = hasMore ? rows.subList(0, page.size()) : rows;

        return new Page<>(onThisPage.stream().map(JpaOrderQuery::toListItem).toList(), hasMore);
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
