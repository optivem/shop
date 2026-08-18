package com.mycompany.myshop.backend.infrastructure.persistence.queries;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.usecases.queries.OrderDetail;
import com.mycompany.myshop.backend.usecases.queries.OrderListItem;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The read side, in SQL. The SELECT list is the projection: these queries fetch the columns the
 * response holds and nothing else, and no {@code OrderJpaEntity} is ever materialised — so nothing
 * enters the persistence context, nothing is dirty-checked, and no domain constructor runs.
 *
 * <p>Rows come back as {@code Object[]} rather than through a {@code SELECT new ...} constructor
 * expression because {@code status} is stored as an enum and leaves this class as a plain string:
 * the projection records are not allowed to name a domain type. The mapping is one place, directly
 * under the SELECT list it mirrors.
 */
@Component
public class JpaOrderQuery implements OrderQuery {

    private static final String LIST_SELECT = """
            SELECT o.orderNumber, o.orderTimestamp, o.sku, o.country, o.quantity,
                   o.totalPrice, o.status, o.appliedCouponCode
            FROM OrderJpaEntity o
            """;

    private static final String LIST_FILTER =
            "WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%')) ";

    // order_number is the tiebreaker rather than the surrogate id, because the id is not allowed out
    // of this package -- and it is the key Chunk C's keyset cursor will need anyway.
    private static final String LIST_ORDER = "ORDER BY o.orderTimestamp DESC, o.orderNumber DESC";

    private static final String DETAIL_SELECT = """
            SELECT o.orderNumber, o.orderTimestamp, o.sku, o.quantity, o.unitPrice,
                   o.basePrice, o.discountRate, o.discountAmount, o.subtotalPrice,
                   o.taxRate, o.taxAmount, o.totalPrice, o.status, o.country,
                   o.appliedCouponCode
            FROM OrderJpaEntity o
            WHERE o.orderNumber = :orderNumber
            """;

    private static final String ORDER_NUMBER = "orderNumber";

    private final EntityManager entityManager;

    public JpaOrderQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<OrderListItem> listOrders(String orderNumberFilter) {
        var filter = orderNumberFilter == null ? null : orderNumberFilter.trim();
        var filtered = filter != null && !filter.isEmpty();

        var query = entityManager.createQuery(
                LIST_SELECT + (filtered ? LIST_FILTER : "") + LIST_ORDER, Object[].class);
        if (filtered) {
            query.setParameter(ORDER_NUMBER, filter);
        }

        return query.getResultList().stream()
                .map(JpaOrderQuery::toListItem)
                .toList();
    }

    @Override
    public Optional<OrderDetail> findOrderDetail(String orderNumber) {
        return entityManager.createQuery(DETAIL_SELECT, Object[].class)
                .setParameter(ORDER_NUMBER, orderNumber)
                .getResultList().stream()
                .findFirst()
                .map(JpaOrderQuery::toDetail);
    }

    private static OrderListItem toListItem(Object[] row) {
        return new OrderListItem(
                (String) row[0],
                (Instant) row[1],
                (String) row[2],
                (String) row[3],
                (Integer) row[4],
                (BigDecimal) row[5],
                name(row[6]),
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

    private static String name(Object status) {
        return status == null ? null : ((OrderStatus) status).name();
    }
}
