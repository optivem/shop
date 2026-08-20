package com.mycompany.myshop.backend.usecases.queries;

import java.util.Optional;

// The read side of orders.
//
// Both methods name an intent, not a mechanism. listOrders takes the optional filter and
// the page, so the LIKE and the LIMIT both stay in SQL instead of becoming an
// if and a subList in the use case, and the adapter stays free to answer with one
// statement.
//
// The before-picture is worth keeping in mind: this port used to be
// findAllByOrderByOrderTimestampDesc() on the domain repository -- Spring Data's
// query-derivation DSL spelled into a port whose own javadoc claimed "no Spring Data, no JPA".
// Because the port named a mechanism, "give me all the rows" was the only question it could be
// asked, so filtering and limiting had nowhere to happen but in memory.
//
// findOrderDetail is the projection twin of
// com.mycompany.myshop.backend.domain.repositories.OrderRepository#findByOrderNumber -- the
// same question, two answers. The command side needs the entity, because it is about to ask it to
// decide something; the query side needs the columns, because it is about to serialize them.
public interface OrderQuery {

    // Newest first, one page at a time. A blank or null filter means "no filter"; anything
    // else is matched case-insensitively as a substring of the order number. A null cursor
    // on the page means "start at the newest".
    Page<OrderListItem> listOrders(String orderNumberFilter, PageSpec<OrderCursor> page);

    Optional<OrderDetail> findOrderDetail(String orderNumber);
}
