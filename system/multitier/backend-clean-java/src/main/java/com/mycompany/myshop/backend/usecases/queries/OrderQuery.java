package com.mycompany.myshop.backend.usecases.queries;

import java.util.List;
import java.util.Optional;

/**
 * The read side of orders.
 *
 * <p>Both methods name an intent, not a mechanism. {@code listOrders} takes the optional filter, so
 * the {@code LIKE} stays in SQL instead of becoming an {@code if} in the use case, and the adapter
 * stays free to answer with one statement.
 *
 * <p>{@code findOrderDetail} is the projection twin of
 * {@link com.mycompany.myshop.backend.domain.repositories.OrderRepository#findByOrderNumber} — the
 * same question, two answers. The command side needs the entity, because it is about to ask it to
 * decide something; the query side needs the columns, because it is about to serialize them.
 */
public interface OrderQuery {

    /**
     * Newest first. A blank or {@code null} filter means "no filter"; anything else is matched
     * case-insensitively as a substring of the order number.
     */
    List<OrderListItem> listOrders(String orderNumberFilter);

    Optional<OrderDetail> findOrderDetail(String orderNumber);
}
